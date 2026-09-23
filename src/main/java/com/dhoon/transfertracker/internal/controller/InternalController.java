package com.dhoon.transfertracker.internal.controller;

import com.dhoon.transfertracker.external.football.dto.player.PlayersResponseDto;
import com.dhoon.transfertracker.external.football.dto.playertransfer.PlayerTransferInfoResponseDto;
import com.dhoon.transfertracker.external.football.dto.team.TeamInfoResponseDto;
import com.dhoon.transfertracker.internal.service.PlayerService;
import com.dhoon.transfertracker.internal.service.TeamService;
import com.dhoon.transfertracker.internal.service.TransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class InternalController {

    private final TransferService transferService;
    private final TeamService teamService;
    private final PlayerService playerService;


    @GetMapping("/api/team/{teamId}")
    public ResponseEntity<TeamInfoResponseDto> getTeam(@PathVariable Long teamId) {
        TeamInfoResponseDto response = teamService.getTeam(teamId);

        return ResponseEntity.ok(response);
    }


}
