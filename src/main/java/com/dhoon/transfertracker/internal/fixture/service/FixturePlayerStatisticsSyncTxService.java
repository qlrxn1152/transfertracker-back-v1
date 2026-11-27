package com.dhoon.transfertracker.internal.fixture.service;
import com.dhoon.transfertracker.internal.fixture.domain.*;
import com.dhoon.transfertracker.internal.fixture.dto.record.ApiFootballFixturePlayerStatisticsResponse;
import com.dhoon.transfertracker.internal.fixture.repository.*;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
@Service
@Transactional
@RequiredArgsConstructor
public class FixturePlayerStatisticsSyncTxService {
    private final FixtureRepository fixtureRepository;
    private final FixturePlayerStatisticsRepository repository;
    private final PlayerRepository playerRepository;
    public void syncFixturePlayerStatistics(Long fixtureApiId, List<ApiFootballFixturePlayerStatisticsResponse> records) {
        Fixture fixture = fixtureRepository.findByApiFootballId(fixtureApiId).orElseThrow();
        Map<Long, Team> teams = Map.of(fixture.getHomeTeam().getApiFootballId(), fixture.getHomeTeam(), fixture.getAwayTeam().getApiFootballId(), fixture.getAwayTeam());
        if (!records.stream().map(ApiFootballFixturePlayerStatisticsResponse::teamApiId).collect(Collectors.toSet()).equals(teams.keySet())) throw new IllegalArgumentException("Invalid fixture teams");
        Map<Long, Player> players = playerRepository.findAllByApiFootballIdIn(records.stream().map(ApiFootballFixturePlayerStatisticsResponse::playerApiId).distinct().toList()).stream().collect(Collectors.toMap(Player::getApiFootballId, Function.identity()));
        Map<String, FixturePlayerStatistics> existing = repository.findAllByFixtureId(fixture.getId()).stream().collect(Collectors.toMap(e -> e.getTeam().getId() + ":" + e.getPlayer().getId(), Function.identity()));
        Set<String> seen = new HashSet<>();
        List<FixturePlayerStatistics> created = new ArrayList<>();
        for (ApiFootballFixturePlayerStatisticsResponse r : records) {
            Team team = teams.get(r.teamApiId());
            Player player = players.computeIfAbsent(r.playerApiId(), id -> playerRepository.save(Player.of(r.playerName(), id)));
            String key = team.getId() + ":" + player.getId();
            if (!seen.add(key)) throw new IllegalArgumentException("Duplicate statistic");
            FixturePlayerStatistics entity = existing.get(key);
            if (entity == null) { entity = FixturePlayerStatistics.of(fixture, team, player); created.add(entity); }
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
                    r.substitute(),
                    r.offsides(),
                    r.passesAccurate()
            );
        }
        repository.saveAll(created);
        if (List.of(FixtureStatus.FINISHED, FixtureStatus.AFTER_EXTRA_TIME, FixtureStatus.AFTER_PENALTY).contains(fixture.getStatus())) fixture.completePlayerStatisticsSync();
    }
}
