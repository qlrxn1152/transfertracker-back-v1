package com.dhoon.transfertracker.internal.player.translation.controller;

import com.dhoon.transfertracker.internal.player.translation.dto.response.PlayerNameTranslationResponseDto;
import com.dhoon.transfertracker.internal.player.translation.service.impl.PlayerNameTranslationOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class PlayerNameTranslationController {

    private final PlayerNameTranslationOrchestrationService playerNameTranslationOrchestrationService;

    @PostMapping("/external/openai/player-names/translate")
    public ResponseEntity<PlayerNameTranslationResponseDto> translatePlayerNames() {
        PlayerNameTranslationResponseDto response = playerNameTranslationOrchestrationService.translate();

        return ResponseEntity.ok(response);
    }

}
