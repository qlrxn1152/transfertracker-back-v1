package com.dhoon.transfertracker.internal.player.controller;

import com.dhoon.transfertracker.internal.player.dto.response.PlayerItemResponseDto;
import com.dhoon.transfertracker.internal.player.dto.response.PlayersResponseDto;
import com.dhoon.transfertracker.internal.player.service.PlayerService;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
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
    public ResponseEntity<PlayersResponseDto> getPlayers(
            @Min(0) @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "") String keyWord, @RequestParam(required = false) LeagueCode leagueCode, @RequestParam(required = false) Long teamId)
    {

        PlayersResponseDto response = playerService.getPlayers(page, keyWord, leagueCode, teamId);

        return ResponseEntity.ok(response);
    }


}
