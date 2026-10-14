package com.dhoon.transfertracker.internal.player.sync.controller;

import com.dhoon.transfertracker.internal.player.sync.dto.response.PlayerSaveResponseDto;
import com.dhoon.transfertracker.internal.player.sync.service.impl.PlayerSyncOrchestrationService;
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
public class PlayerSyncController {

    private final PlayerSyncOrchestrationService playerSyncOrchestrationService;

    @PostMapping("/external/api/player/{playerApiId}")
    public ResponseEntity<PlayerSaveResponseDto> savePlayer(@PathVariable Long playerApiId) {
        PlayerSaveResponseDto response = playerSyncOrchestrationService.syncPlayer(playerApiId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}
