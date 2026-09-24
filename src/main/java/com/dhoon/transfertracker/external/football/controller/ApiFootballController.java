package com.dhoon.transfertracker.external.football.controller;

import com.dhoon.transfertracker.external.football.client.ApiFootballClient;
import com.dhoon.transfertracker.external.football.dto.PlayerSaveResponseDto;
import com.dhoon.transfertracker.external.football.dto.TeamPlayersSaveResponseDto;
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

    @PostMapping("/external/api/player/{playerApiId}")
    public ResponseEntity<PlayerSaveResponseDto> savePlayer(@PathVariable Long playerApiId) {
        PlayerSaveResponseDto response = apiFootballClient.savePlayer(playerApiId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/external/api/teams/{teamApiID}")
    public ResponseEntity<TeamSaveResponseDto> saveTeam(@PathVariable Long teamApiID) {
        TeamSaveResponseDto response = apiFootballClient.saveTeam(teamApiID);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/external/api/transfer/{playerApiId}")
    public ResponseEntity<TransferSaveResponseDto> savePlayerTransfer(@PathVariable Long playerApiId) {
        TransferSaveResponseDto response = apiFootballClient.savePlayerTransfer(playerApiId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/external/api/team/players/{teamApiID}")
    public ResponseEntity<TeamPlayersSaveResponseDto> saveTeamPlayers(@PathVariable Long teamApiID) {
        TeamPlayersSaveResponseDto response = apiFootballClient.saveTeamPlayers(teamApiID);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
