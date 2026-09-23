package com.dhoon.transfertracker.external.football.controller;

import com.dhoon.transfertracker.external.football.client.ApiFootballClient;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class ApiFootballController {

    private final ApiFootballClient apiFootballClient;

    @PostMapping("/external/api/transfers/{playerId}")
    public ResponseEntity<PlayerTransferInfoResponseDto> a(@PathVariable Long playerId) {
        PlayerTransferInfoResponseDto response = apiFootballClient.savePlayerTransfer(playerId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/external/api/teams/{teamId}")
    public ResponseEntity<TeamInfoResponseDto> b(@PathVariable Long teamId) {
        TeamInfoResponseDto response = apiFootballClient.saveTeam(teamId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/external/api/player/{playerId}")
    public ResponseEntity<PlayerItemResponseDto> c(@PathVariable Long playerId) {
        PlayerItemResponseDto response = apiFootballClient.savePlayer(playerId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/external/api/team/players/{teamId}")
    public ResponseEntity<PlayersResponseDto> d(@PathVariable Long teamId) {
        PlayersResponseDto response = apiFootballClient.saveTeamPlayers(teamId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/external/api/teams/country/{country}")
    public ResponseEntity<?> e(@PathVariable String country) {
        ResponseEntity<?> response = apiFootballClient.saveCountryTeams(country);// 국가대표팀이 아닌, country 에 속한 팀들 저장

        return ResponseEntity.status(HttpStatus.CREATED).body(response);

    }
}
