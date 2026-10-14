package com.dhoon.transfertracker.external.football.controller;

import com.dhoon.transfertracker.external.football.dto.*;
import com.dhoon.transfertracker.external.football.service.FootballSyncService;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.transfer.query.dto.response.PlayerTransfersResponseDto;
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




}
