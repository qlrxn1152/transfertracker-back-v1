package com.dhoon.transfertracker.internal.transferpost.translation.controller;

import com.dhoon.transfertracker.external.openai.dto.response.TranslationBatchResult;
import com.dhoon.transfertracker.internal.transferpost.translation.service.impl.TransferPostTranslationOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class TransferPostTranslationController {

    private final TransferPostTranslationOrchestrationService translationService;



    @PostMapping("/external/api/translate")
    public ResponseEntity<TranslationBatchResult> translate() {
        TranslationBatchResult response = translationService.translate();

        return ResponseEntity.ok(response);
    }


}
