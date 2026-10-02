package com.dhoon.transfertracker.external.openai.controller;

import com.dhoon.transfertracker.external.openai.client.OpenAiClient;
import com.dhoon.transfertracker.external.openai.dto.request.TranslationTarget;
import com.dhoon.transfertracker.external.openai.dto.response.TranslationBatchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
public class OpenAiController {

    private final OpenAiClient openAiClient;

//    @PostMapping("/external/test222")
    public TranslationBatchResult test222(@RequestBody List<TranslationTarget> targets) {

        return openAiClient.translate(targets);
    }
}
