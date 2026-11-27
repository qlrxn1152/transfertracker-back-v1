package com.dhoon.transfertracker.internal.player.service;
import com.dhoon.transfertracker.internal.player.dto.record.*;
import com.dhoon.transfertracker.internal.player.domain.PlayerRankingType;
import com.dhoon.transfertracker.internal.player.repository.*;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PlayerStatisticsQueryService {
    private final PlayerSeasonStatisticsRepository seasonRepository;
    private final PlayerRankingRepository rankingRepository;
    public List<PlayerSeasonStatisticsResponse> getSeasonStatistics(Long playerId) { return seasonRepository.findAllByPlayerId(playerId).stream().map(PlayerSeasonStatisticsResponse::from).toList(); }
    public List<PlayerRankingResponse> getRanking(LeagueCode league, Integer season, PlayerRankingType type) { return rankingRepository.findSnapshot(league, season, type).stream().map(PlayerRankingResponse::from).toList(); }
}
