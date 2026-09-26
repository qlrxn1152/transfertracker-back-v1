package com.dhoon.transfertracker.external.football.controller;

import com.dhoon.transfertracker.external.football.client.ApiFootballClient;
import com.dhoon.transfertracker.external.football.dto.PlayerSaveResponseDto;
import com.dhoon.transfertracker.external.football.dto.TeamSaveResponseDto;
import com.dhoon.transfertracker.external.football.dto.TransferSaveResponseDto;
import com.dhoon.transfertracker.internal.domain.LeagueCode;
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
public class ApiFootballController {

    private final ApiFootballClient apiFootballClient;

    @PostMapping("/external/api/player/{playerApiId}")
    public ResponseEntity<PlayerSaveResponseDto> savePlayer(@PathVariable Long playerApiId) {
        PlayerSaveResponseDto response = apiFootballClient.syncPlayer(playerApiId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/external/api/teams/{teamApiId}")
    public ResponseEntity<TeamSaveResponseDto> saveTeam(@PathVariable Long teamApiId) {
        TeamSaveResponseDto response = apiFootballClient.syncTeam(teamApiId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @PostMapping("/external/api/transfer/{playerApiId}")
    public ResponseEntity<String> savePlayerTransfer(@PathVariable Long playerApiId) {
        String response = apiFootballClient.syncPlayerTransfers(playerApiId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/external/api/team/players/{teamApiId}")
    public ResponseEntity<String> saveTeamPlayers(@PathVariable Long teamApiId) {
        String response = apiFootballClient.syncTeamPlayers(teamApiId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/external/api/transfers/team/{teamApiId}")
    public ResponseEntity<String> saveTeamTransfers(@PathVariable Long teamApiId) {
        String response = apiFootballClient.saveTeamTransfers(teamApiId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/external/api/teams/league/{leagueCode}")
    public ResponseEntity<String> syncLeagueTeams(@PathVariable LeagueCode leagueCode) {
        String response = apiFootballClient.syncLeagueTeams(leagueCode);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }





}
