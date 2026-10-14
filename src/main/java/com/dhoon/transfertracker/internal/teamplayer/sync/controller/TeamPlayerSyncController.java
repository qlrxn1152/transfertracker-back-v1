package com.dhoon.transfertracker.internal.teamplayer.sync.controller;

import com.dhoon.transfertracker.external.football.dto.TeamPlayersSaveResponseDto;
import com.dhoon.transfertracker.internal.teamplayer.sync.service.impl.TeamPlayerSyncOrchestrationService;
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
public class TeamPlayerSyncController {

    private final TeamPlayerSyncOrchestrationService teamPlayerSyncOrchestrationService;

    @PostMapping("/external/api/team/players/{teamApiId}")
    public ResponseEntity<TeamPlayersSaveResponseDto> saveTeamPlayers(@PathVariable Long teamApiId) {
        TeamPlayersSaveResponseDto response = teamPlayerSyncOrchestrationService.syncTeamPlayers(teamApiId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
