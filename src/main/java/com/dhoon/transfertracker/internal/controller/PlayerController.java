package com.dhoon.transfertracker.internal.controller;

import com.dhoon.transfertracker.internal.dto.response.PlayerItemResponseDto;
import com.dhoon.transfertracker.internal.dto.response.PlayersResponseDto;
import com.dhoon.transfertracker.internal.service.PlayerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerService playerService;

    @GetMapping("/api/player/{playerId}")
    public ResponseEntity<PlayerItemResponseDto> getPlayer(@PathVariable Long playerId) {
        PlayerItemResponseDto response = playerService.getPlayer(playerId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/players")
    public ResponseEntity<PlayersResponseDto> getPlayers() {
        PlayersResponseDto response = playerService.getPlayers();

        return ResponseEntity.ok(response);
    }


}
