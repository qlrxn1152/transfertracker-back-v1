package com.dhoon.transfertracker.internal.player.controller;
import com.dhoon.transfertracker.internal.player.dto.record.*;
import com.dhoon.transfertracker.internal.player.domain.PlayerRankingType;
import com.dhoon.transfertracker.internal.player.service.PlayerStatisticsQueryService;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.List;
@RestController
@RequiredArgsConstructor
public class PlayerStatisticsController {
    private final PlayerStatisticsQueryService queryService;
    @GetMapping("/api/players/{playerId}/season-statistics")
    public List<PlayerSeasonStatisticsResponse> getSeasonStatistics(@PathVariable("playerId") Long playerId) { return queryService.getSeasonStatistics(playerId); }
    @GetMapping("/api/player-rankings")
    public List<PlayerRankingResponse> getRanking(@RequestParam LeagueCode leagueCode, @RequestParam(required = false) Integer season, @RequestParam PlayerRankingType type) {
        if (season == null) season = leagueCode.resolveSeason(LocalDate.now(ZoneId.of("Asia/Seoul")));
        return queryService.getRanking(leagueCode, season, type);
    }
}
