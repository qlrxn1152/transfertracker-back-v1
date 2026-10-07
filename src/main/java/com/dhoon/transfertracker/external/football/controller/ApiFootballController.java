package com.dhoon.transfertracker.external.football.controller;

import com.dhoon.transfertracker.external.football.dto.*;
import com.dhoon.transfertracker.external.football.service.FootballSyncService;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.transfer.dto.response.PlayerTransfersResponseDto;
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

    private final FootballSyncService footballSyncService;

    @PostMapping("/external/api/player/{playerApiId}")
    public ResponseEntity<PlayerSaveResponseDto> savePlayer(@PathVariable Long playerApiId) {
        PlayerSaveResponseDto response = footballSyncService.syncPlayer(playerApiId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/external/api/teams/{teamApiId}")
    public ResponseEntity<TeamSaveResponseDto> saveTeam(@PathVariable Long teamApiId) {
        TeamSaveResponseDto response = footballSyncService.syncTeam(teamApiId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/external/api/transfer/{playerApiId}")
    public ResponseEntity<PlayerTransfersResponseDto> savePlayerTransfer(@PathVariable Long playerApiId) {
        PlayerTransfersResponseDto response = footballSyncService.syncPlayerTransfers(playerApiId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/external/api/team/players/{teamApiId}")
    public ResponseEntity<TeamPlayersSaveResponseDto> saveTeamPlayers(@PathVariable Long teamApiId) {
        TeamPlayersSaveResponseDto response = footballSyncService.syncTeamPlayers(teamApiId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/external/api/teams/league/{leagueCode}")
    public ResponseEntity<LeagueTeamsResponseDto> syncLeagueTeams(@PathVariable LeagueCode leagueCode) {
        LeagueTeamsResponseDto response = footballSyncService.syncLeagueTeams(leagueCode);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/external/api/transfers/team/{teamApiId}")
    public ResponseEntity<TeamTransfersSaveResponseDto> saveTeamTransfers(@PathVariable Long teamApiId) {
        TeamTransfersSaveResponseDto response = footballSyncService.saveTeamTransfers(teamApiId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}
