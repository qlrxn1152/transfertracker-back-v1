package com.dhoon.transfertracker.internal.fixture.controller;

import com.dhoon.transfertracker.internal.fixture.dto.record.FixtureResponse;
import com.dhoon.transfertracker.internal.fixture.service.FixtureQueryService;
import com.dhoon.transfertracker.internal.fixture.service.FixtureSyncService;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
public class FixtureController {

    private final FixtureSyncService fixtureSyncService;
    private final FixtureQueryService fixtureQueryService;

    @PostMapping("/api/admin/fixtures/sync")
    public ResponseEntity<Void> syncFixtures(@RequestParam LeagueCode leagueCode, @RequestParam Integer season) {
        fixtureSyncService.syncLeagueFixtures(leagueCode, season);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/fixtures")
    public ResponseEntity<List<FixtureResponse>> getDateFixtures(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, @RequestParam(required = false) LeagueCode leagueCode) {
        List<FixtureResponse> response = fixtureQueryService.getFixturesByDate(date, leagueCode);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/fixtures/teams/{teamId}")
    public ResponseEntity<Page<FixtureResponse>> getTeamFixtures(@PathVariable Long teamId, @RequestParam(defaultValue = "0") int page) {
        Page<FixtureResponse> response = fixtureQueryService.getTeamFixtures(teamId, page);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/fixtures/{fixtureId}")
    public ResponseEntity<FixtureResponse> getFixture(@PathVariable Long fixtureId) {
        FixtureResponse response = fixtureQueryService.getFixture(fixtureId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/fixtures/teams/{teamId}/upcoming")
    public ResponseEntity<List<FixtureResponse>> getTeamUpcomingFixtures(@PathVariable Long teamId) {
        List<FixtureResponse> response = fixtureQueryService.getTeamUpcomingFixtures(teamId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/fixtures/teams/{teamId}/recent")
    public ResponseEntity<List<FixtureResponse>> getTeamRecentFixtures(@PathVariable Long teamId) {
        List<FixtureResponse> response = fixtureQueryService.getTeamRecentFixtures(teamId);

        return ResponseEntity.ok(response);
    }
}
