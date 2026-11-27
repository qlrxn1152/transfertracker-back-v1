package com.dhoon.transfertracker.internal.player.service;
import com.dhoon.transfertracker.internal.player.domain.*;
import com.dhoon.transfertracker.internal.player.repository.*;
import com.dhoon.transfertracker.internal.player.dto.record.ApiFootballPlayerRankingResponse;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
@Service
@Transactional
@RequiredArgsConstructor
public class PlayerRankingSyncTxService {
    private final PlayerRankingRepository repository;
    private final PlayerRepository playerRepository;
    public void sync(LeagueCode league, Integer season, PlayerRankingType type, List<ApiFootballPlayerRankingResponse> records) {
        if (records.isEmpty()) return;
        Map<Long, Player> players = playerRepository.findAllByApiFootballIdIn(records.stream().map(ApiFootballPlayerRankingResponse::playerApiId).distinct().toList()).stream().collect(Collectors.toMap(Player::getApiFootballId, Function.identity()));
        List<PlayerRanking> rankings = new ArrayList<>();
        for (ApiFootballPlayerRankingResponse r : records) {
            Player player = players.computeIfAbsent(r.playerApiId(), id -> playerRepository.save(Player.of(r.playerName(), id)));
            PlayerRanking ranking = PlayerRanking.of(player, league, season, type);
            ranking.update(r.ranking(), r.value());
            rankings.add(ranking);
        }
        repository.deleteSnapshot(league, season, type);
        repository.saveAll(rankings);
    }
}
