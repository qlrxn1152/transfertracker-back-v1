package com.dhoon.transfertracker.internal.team.sync.controller;

import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.sync.dto.response.LeagueTeamsResponseDto;
import com.dhoon.transfertracker.internal.team.sync.dto.response.TeamSaveResponseDto;
import com.dhoon.transfertracker.internal.team.sync.service.impl.TeamSyncOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class TeamSyncController {

    private final TeamSyncOrchestrationService teamSyncOrchestrationService;

    @PostMapping("/external/api/teams/{teamApiId}")
    public ResponseEntity<TeamSaveResponseDto> saveTeam(@PathVariable Long teamApiId) {
        TeamSaveResponseDto response = teamSyncOrchestrationService.syncTeam(teamApiId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @PostMapping("/external/api/teams/league/{leagueCode}")
    public ResponseEntity<LeagueTeamsResponseDto> syncLeagueTeams(@PathVariable LeagueCode leagueCode) {
        LeagueTeamsResponseDto response = teamSyncOrchestrationService.syncLeagueTeams(leagueCode);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
