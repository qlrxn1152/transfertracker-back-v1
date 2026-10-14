package com.dhoon.transfertracker.internal.teamplayer.query.controller;

import com.dhoon.transfertracker.internal.teamplayer.query.dto.response.TeamPlayersResponseDto;
import com.dhoon.transfertracker.internal.teamplayer.query.service.impl.TeamPlayerQueryOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class TeamPlayerQueryController {

    private final TeamPlayerQueryOrchestrationService teamPlayerQueryOrchestrationService;


    @GetMapping("/api/team/player/{teamId}")
    public ResponseEntity<TeamPlayersResponseDto> getTeamPlayers(@PathVariable Long teamId) {
        TeamPlayersResponseDto response = teamPlayerQueryOrchestrationService.getTeamPlayers(teamId);

        return ResponseEntity.ok(response);
    }





}
