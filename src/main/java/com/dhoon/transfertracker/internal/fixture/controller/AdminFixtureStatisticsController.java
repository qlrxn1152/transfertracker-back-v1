package com.dhoon.transfertracker.internal.fixture.controller;
import com.dhoon.transfertracker.internal.fixture.service.FixturePlayerStatisticsSyncService;
import com.dhoon.transfertracker.internal.fixture.service.FixtureStatisticsSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/fixtures")
public class AdminFixtureStatisticsController {
    private final FixtureStatisticsSyncService statisticsSyncService;
    private final FixturePlayerStatisticsSyncService playerStatisticsSyncService;
    @PostMapping("/{fixtureApiId}/statistics/sync")
    public ResponseEntity<Void> syncStatistics(@PathVariable("fixtureApiId") Long fixtureApiId) { statisticsSyncService.syncFixtureStatistics(fixtureApiId); return ResponseEntity.noContent().build(); }
    @PostMapping("/{fixtureApiId}/player-statistics/sync")
    public ResponseEntity<Void> syncPlayerStatistics(@PathVariable("fixtureApiId") Long fixtureApiId) { playerStatisticsSyncService.syncFixturePlayerStatistics(fixtureApiId); return ResponseEntity.noContent().build(); }
}
