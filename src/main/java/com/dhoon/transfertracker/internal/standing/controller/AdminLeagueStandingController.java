package com.dhoon.transfertracker.internal.standing.controller;

import com.dhoon.transfertracker.internal.standing.service.LeagueStandingSyncService;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AdminLeagueStandingController {

    private final LeagueStandingSyncService leagueStandingSyncService;

    @PostMapping("/api/admin/standings/sync")
    public ResponseEntity<Void> syncLeagueStandings(
            @RequestParam LeagueCode leagueCode,
            @RequestParam Integer season
    ) {

        leagueStandingSyncService.syncLeagueStandings(
                leagueCode,
                season
        );

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/admin/standings/sync/all")
    public ResponseEntity<Void> syncAllLeagueStandings() {

        leagueStandingSyncService.syncAllLeagueStandings();

        return ResponseEntity.noContent().build();
    }
}