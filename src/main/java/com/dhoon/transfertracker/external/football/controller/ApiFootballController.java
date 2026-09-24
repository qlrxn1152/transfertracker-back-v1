package com.dhoon.transfertracker.external.football.controller;

import com.dhoon.transfertracker.external.football.client.ApiFootballClient;
import com.dhoon.transfertracker.external.football.dto.PlayerSaveResponseDto;
import com.dhoon.transfertracker.external.football.dto.TeamSaveResponseDto;
import com.dhoon.transfertracker.external.football.dto.TransferSaveResponseDto;
import com.dhoon.transfertracker.external.football.dto.player.PlayerItemResponseDto;
import com.dhoon.transfertracker.external.football.dto.player.PlayersResponseDto;
import com.dhoon.transfertracker.external.football.dto.playertransfer.PlayerTransferInfoResponseDto;
import com.dhoon.transfertracker.external.football.dto.team.TeamInfoResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@RestController
@Slf4j
@RequiredArgsConstructor
public class ApiFootballController {

    private final ApiFootballClient apiFootballClient;

    @PostMapping("/external/api/player/{playerId}")
    public ResponseEntity<PlayerSaveResponseDto> savePlayer(@PathVariable Long playerId) {
        PlayerSaveResponseDto response = apiFootballClient.savePlayer(playerId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/external/api/teams/{teamId}")
    public ResponseEntity<TeamSaveResponseDto> saveTeam(@PathVariable Long teamId) {
        TeamSaveResponseDto response = apiFootballClient.saveTeam(teamId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/external/api/transfer/{playerId}")
    public ResponseEntity<TransferSaveResponseDto> savePlayerTransfer(@PathVariable Long playerId) {
        TransferSaveResponseDto response = apiFootballClient.savePlayerTransfer(playerId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }













}
