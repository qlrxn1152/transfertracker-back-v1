package com.dhoon.transfertracker.internal.fixture.controller;

import com.dhoon.transfertracker.internal.fixture.service.FixtureSyncService;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class FixtureController {

    private final FixtureSyncService fixtureSyncService;

    @PostMapping("/api/admin/fixtures/sync")
    public ResponseEntity<Void> syncFixtures(@RequestParam LeagueCode leagueCode, @RequestParam Integer season) {
        fixtureSyncService.syncLeagueFixtures(leagueCode, season);

        return ResponseEntity.noContent().build();
    }
}
