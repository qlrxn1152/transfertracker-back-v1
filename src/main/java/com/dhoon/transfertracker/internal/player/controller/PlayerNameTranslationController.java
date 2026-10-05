package com.dhoon.transfertracker.internal.player.controller;

import com.dhoon.transfertracker.internal.player.dto.response.PlayerNameTranslationResponseDto;
import com.dhoon.transfertracker.internal.player.service.impl.PlayerNameTranslationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class PlayerNameTranslationController {
    private final PlayerNameTranslationService playerNameTranslationService;

    @PostMapping("/external/openai/player-names/translate")
    public ResponseEntity<PlayerNameTranslationResponseDto> translatePlayerNames() {
        PlayerNameTranslationResponseDto response = playerNameTranslationService.translate();

        return ResponseEntity.ok(response);
    }
}
