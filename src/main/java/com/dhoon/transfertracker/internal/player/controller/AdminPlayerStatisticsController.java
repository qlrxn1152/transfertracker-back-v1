package com.dhoon.transfertracker.internal.player.controller;
import com.dhoon.transfertracker.internal.player.service.*;
import com.dhoon.transfertracker.internal.player.domain.PlayerRankingType;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminPlayerStatisticsController {
    private final PlayerSeasonStatisticsSyncService seasonSyncService;
    private final PlayerRankingSyncService rankingSyncService;
    @PostMapping("/players/{playerApiId}/season-statistics/sync")
    public ResponseEntity<Void> syncSeason(@PathVariable("playerApiId") Long playerApiId, @RequestParam Integer season) { seasonSyncService.syncPlayerSeasonStatistics(playerApiId, season); return ResponseEntity.noContent().build(); }
    @PostMapping("/player-rankings/sync")
    public ResponseEntity<Void> syncRanking(@RequestParam LeagueCode leagueCode, @RequestParam Integer season, @RequestParam PlayerRankingType type) { rankingSyncService.sync(leagueCode, season, type); return ResponseEntity.noContent().build(); }
    @PostMapping("/player-rankings/sync/all")
    public ResponseEntity<Void> syncAll() { rankingSyncService.syncAll(); return ResponseEntity.noContent().build(); }
    @PostMapping("/player-season-statistics/sync")
    public ResponseEntity<Void> syncLeague(@RequestParam LeagueCode leagueCode, @RequestParam Integer season) { seasonSyncService.syncLeague(leagueCode, season); return ResponseEntity.noContent().build(); }
    @PostMapping("/player-season-statistics/sync/all")
    public ResponseEntity<Void> syncAllSeasons() { seasonSyncService.syncAllLeagues(); return ResponseEntity.noContent().build(); }
}
