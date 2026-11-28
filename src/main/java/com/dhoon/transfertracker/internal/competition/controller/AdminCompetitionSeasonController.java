package com.dhoon.transfertracker.internal.competition.controller;

import com.dhoon.transfertracker.internal.competition.service.CompetitionSeasonSyncService;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/competitions")
public class AdminCompetitionSeasonController {

    private final CompetitionSeasonSyncService competitionSeasonSyncService;

    @PostMapping("/seasons/sync")
    public ResponseEntity<Void> syncCompetitionSeasons(
            @RequestParam LeagueCode leagueCode
    ) {

        competitionSeasonSyncService.syncCompetitionSeasons(
                leagueCode
        );

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/seasons/sync/all")
    public ResponseEntity<Void> syncAllCompetitionSeasons() {

        competitionSeasonSyncService.syncAllCompetitionSeasons();

        return ResponseEntity.noContent().build();
    }
}