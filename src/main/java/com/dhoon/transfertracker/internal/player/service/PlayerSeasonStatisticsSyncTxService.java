package com.dhoon.transfertracker.internal.player.service;
import com.dhoon.transfertracker.internal.player.domain.*;
import com.dhoon.transfertracker.internal.player.repository.*;
import com.dhoon.transfertracker.internal.player.dto.record.ApiFootballPlayerSeasonStatisticsResponse;
import com.dhoon.transfertracker.internal.team.domain.*;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
@Service
@Transactional
@RequiredArgsConstructor
public class PlayerSeasonStatisticsSyncTxService {
    private final PlayerSeasonStatisticsRepository repository;
    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    public void sync(List<ApiFootballPlayerSeasonStatisticsResponse> records) {
        Map<Long, Team> teams = teamRepository.findAllByApiFootballIdIn(records.stream().map(ApiFootballPlayerSeasonStatisticsResponse::teamApiId).distinct().toList()).stream().collect(Collectors.toMap(Team::getApiFootballId, Function.identity()));
        Map<Long, Player> players = playerRepository.findAllByApiFootballIdIn(records.stream().map(ApiFootballPlayerSeasonStatisticsResponse::playerApiId).distinct().toList()).stream().collect(Collectors.toMap(Player::getApiFootballId, Function.identity()));
        Map<String, PlayerSeasonStatistics> existing = new HashMap<>();
        Set<String> loaded = new HashSet<>();
        Set<String> seen = new HashSet<>();
        List<PlayerSeasonStatistics> created = new ArrayList<>();
        for (ApiFootballPlayerSeasonStatisticsResponse r : records) {
            Team team = teams.get(r.teamApiId());
            if (team == null) throw new IllegalStateException("Unknown team: " + r.teamApiId());
            Player player = players.computeIfAbsent(r.playerApiId(), id -> playerRepository.save(Player.of(r.playerName(), id)));
            LeagueCode league = LeagueCode.fromApiFootballLeagueId(r.leagueApiId());
            String scope = player.getId() + ":" + league + ":" + r.season();
            if (loaded.add(scope)) for (PlayerSeasonStatistics e : repository.findExisting(r.playerApiId(), league, r.season())) existing.put(scope + ":" + e.getTeam().getId(), e);
            String key = scope + ":" + team.getId();
            if (!seen.add(key)) throw new IllegalArgumentException("Duplicate season record");
            PlayerSeasonStatistics entity = existing.get(key);
            if (entity == null) { entity = PlayerSeasonStatistics.of(player, team, league, r.season()); created.add(entity); }
            entity.update(
                    r.minutes(),
                    r.number(),
                    r.position(),
                    r.rating(),
                    r.captain(),
                    r.shotsTotal(),
                    r.shotsOn(),
                    r.goals(),
                    r.goalsConceded(),
                    r.assists(),
                    r.saves(),
                    r.passesTotal(),
                    r.passesKey(),
                    r.tacklesTotal(),
                    r.blocks(),
                    r.interceptions(),
                    r.duelsTotal(),
                    r.duelsWon(),
                    r.dribblesAttempts(),
                    r.dribblesSuccess(),
                    r.dribbledPast(),
                    r.foulsDrawn(),
                    r.foulsCommitted(),
                    r.yellowCards(),
                    r.redCards(),
                    r.penaltyWon(),
                    r.penaltyCommitted(),
                    r.penaltyScored(),
                    r.penaltyMissed(),
                    r.penaltySaved(),
                    r.appearances(),
                    r.lineups(),
                    r.substitutesIn(),
                    r.substitutesOut(),
                    r.substitutesBench(),
                    r.yellowRedCards(),
                    r.passesAccuracy()
            );
        }
        repository.saveAll(created);
    }
}
