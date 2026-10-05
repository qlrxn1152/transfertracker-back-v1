package com.dhoon.transfertracker.external.openai.client;

import com.dhoon.transfertracker.external.openai.dto.request.PlayerNameTranslationTarget;
import com.dhoon.transfertracker.external.openai.dto.request.TranslationTarget;
import com.dhoon.transfertracker.external.openai.dto.response.PlayerNameTranslationBatchResult;
import com.dhoon.transfertracker.external.openai.dto.response.TranslationBatchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class OpenAiClient {

    private final RestClient openAiRestClient;
    private final ObjectMapper objectMapper;
    private final String model;

    public OpenAiClient(
            @Qualifier("openAiRestClient") RestClient openAiRestClient,
            ObjectMapper objectMapper,
            @Value("${openai.api.model}") String model
    ) {
        this.openAiRestClient = openAiRestClient;
        this.objectMapper = objectMapper;
        this.model = model;
    }

    public TranslationBatchResult translate(List<TranslationTarget> targets) {

        String input = objectMapper
                .valueToTree(targets)
                .toString();

        Map<String, Object> request = Map.of(
                "model", model,

                "instructions", """
                        Translate the given English football posts into natural Korean.

                        Rules:
                        - Do not summarize or add information.
                        - Preserve player names, club names, transfer fees, dates, and numbers.
                        - Translate football transfer terminology naturally.
                        - Preserve each postId exactly.
                        - Return the translated result for every input post.
                        - Return JSON only.

                        Response format:
                        {
                          "posts": [
                            {
                              "postId": 1,
                              "translatedContent": "번역된 내용"
                            }
                          ]
                        }
                        """,

                "input", input
        );



        JsonNode response = openAiRestClient.post()
                .uri("/v1/responses")
                .body(request)
                .retrieve()
                .body(JsonNode.class);

        String outputText = extractOutputText(response);

        return objectMapper.readValue(
                outputText,
                TranslationBatchResult.class
        );


    }

    private String extractOutputText(JsonNode response) {

        JsonNode outputs = response.get("output");

        for (JsonNode output : outputs) {

            if (!"message".equals(output.get("type").asString())) {
                continue;
            }

            JsonNode contents = output.get("content");

            for (JsonNode content : contents) {

                if ("output_text".equals(content.get("type").asString())) {
                    return content.get("text").asString();
                }
            }
        }

        throw new IllegalStateException(
                "OpenAI 응답에서 output_text를 찾을 수 없습니다."
        );
    }

    public PlayerNameTranslationBatchResult translatePlayerNames(
            List<PlayerNameTranslationTarget> targets
    ) {

        String input =
                objectMapper
                        .valueToTree(
                                targets
                        )
                        .toString();


        Map<String, Object> request =
                Map.of(
                        "model",
                        model,

                        "instructions",
                        """
                        Convert football player names into natural Korean names
                        commonly understandable to Korean football fans.
    
                        Rules:
                        - playerName is the value to convert.
                        - teamName is context only for identifying the player.
                        - Never include teamName in playerNameKo.
                        - Do not translate the meaning of a person's name.
                        - Use natural Korean transliteration for foreign players.
                        - For well-known football players, prefer the commonly used Korean spelling.
                        - For Korean players written in Roman letters, restore the natural Korean name when confidently identifiable.
                        - Never invent additional player information.
                        - Preserve every playerId exactly.
                        - Return one result for every input player.
                        - Do not omit or duplicate playerId.
                        - Return JSON only.
    
                        Response format:
                        {
                          "players": [
                            {
                              "playerId": 1,
                              "playerNameKo": "브루노 페르난데스"
                            }
                          ]
                        }
                        """,

                        "input",
                        input
                );


        JsonNode response =
                openAiRestClient
                        .post()
                        .uri(
                                "/v1/responses"
                        )
                        .body(
                                request
                        )
                        .retrieve()
                        .body(
                                JsonNode.class
                        );


        String outputText =
                extractOutputText(
                        response
                );


        return objectMapper.readValue(
                outputText,
                PlayerNameTranslationBatchResult.class
        );
    }
}