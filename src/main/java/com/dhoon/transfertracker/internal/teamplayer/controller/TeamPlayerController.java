package com.dhoon.transfertracker.internal.teamplayer.controller;

import com.dhoon.transfertracker.internal.teamplayer.dto.response.TeamPlayersResponseDto;
import com.dhoon.transfertracker.internal.teamplayer.service.TeamPlayerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class TeamPlayerController {

    private final TeamPlayerService teamPlayerService;

    @GetMapping("/api/team/player/{teamId}")
    public ResponseEntity<TeamPlayersResponseDto> getTeamPlayers(@PathVariable Long teamId) {
        TeamPlayersResponseDto response = teamPlayerService.getTeamPlayers(teamId);

        return ResponseEntity.ok(response);
    }





}
