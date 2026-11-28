package com.dhoon.transfertracker.internal.team.sync.controller;

import com.dhoon.transfertracker.internal.team.sync.dto.FootballDataSyncProgress;
import com.dhoon.transfertracker.internal.team.sync.dto.record.FootballDataSyncResponse;
import com.dhoon.transfertracker.internal.team.sync.dto.request.TeamCollectionImportRequest;
import com.dhoon.transfertracker.internal.team.sync.dto.response.TeamCollectionImportResponse;
import com.dhoon.transfertracker.internal.team.sync.service.impl.FootballDataSyncOrchestrationService;
import com.dhoon.transfertracker.internal.team.sync.service.impl.TeamCollectionImportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/football-data")
public class FootballDataSyncController {

    private final FootballDataSyncOrchestrationService
            footballDataSyncOrchestrationService;
    private final TeamCollectionImportService teamCollectionImportService;

    @PostMapping("/teams/import-collection")
    public TeamCollectionImportResponse importCollectionTeams(
            @Valid @RequestBody TeamCollectionImportRequest request
    ) {
        return teamCollectionImportService.importTeams(request);
    }

    @PostMapping("/sync/all")
    public FootballDataSyncResponse syncAll() {

        return footballDataSyncOrchestrationService.syncAll();
    }

    @PostMapping("/sync/teams")
    public FootballDataSyncResponse syncTeams() {

        return footballDataSyncOrchestrationService.syncTeams();
    }

    @PostMapping("/sync/teams/league/{leagueApiId}")
    public FootballDataSyncResponse syncLeagueTeams(
            @PathVariable Long leagueApiId
    ) {
        return footballDataSyncOrchestrationService
                .syncLeagueTeams(leagueApiId);
    }

    @PostMapping("/sync/players")
    public FootballDataSyncResponse syncPlayers() {

        return footballDataSyncOrchestrationService.syncPlayers();
    }

    @PostMapping("/sync/transfers")
    public FootballDataSyncResponse syncTransfers() {

        return footballDataSyncOrchestrationService.syncTransfers();
    }

    @PostMapping("/sync/league/{leagueApiId}")
    public FootballDataSyncResponse syncLeague(
            @PathVariable Long leagueApiId
    ) {

        return footballDataSyncOrchestrationService
                .syncLeague(leagueApiId);
    }

    // 동기화 진행률 조회
    @GetMapping("/sync/progress")
    public FootballDataSyncProgress getProgress() {

        return footballDataSyncOrchestrationService
                .getProgress();
    }

    // 동기화 중단 요청
    @PostMapping("/sync/stop")
    public ResponseEntity<Void> stopSync() {

        footballDataSyncOrchestrationService.stopSync();

        return ResponseEntity.accepted().build();
    }
}
