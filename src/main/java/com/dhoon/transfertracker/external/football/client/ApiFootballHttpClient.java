package com.dhoon.transfertracker.external.football.client;

import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

@Slf4j
@Component
public class ApiFootballHttpClient {

    private final RestClient footballRestClient;

    public ApiFootballHttpClient(@Qualifier("footballRestClient") RestClient footballRestClient) {
        this.footballRestClient = footballRestClient;
    }

    public JsonNode callExternalPlayerApi(Long playerApiId) {
        JsonNode node = footballRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/players")
                        .path("/profiles")
                        .queryParam("player", playerApiId)
                        .build()
                )
                .retrieve()
                .body(JsonNode.class);

        return node.get("response").get(0).get("player");
    }

    public JsonNode callExternalTeamApi(Long teamApiID) {
        JsonNode node = footballRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/teams")
                        .queryParam("id", teamApiID)
                        .build()
                )
                .retrieve()
                .body(JsonNode.class);

        return node.get("response").get(0).get("team");
    }

    public JsonNode callExternalPlayerTransferApi(Long playerApiId) {
        return footballRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/transfers")
                        .queryParam("player", playerApiId)
                        .build()
                )
                .retrieve()
                .body(JsonNode.class);
    }

    public JsonNode callExternalTeamPlayersApi(Long teamApiId) {
        return footballRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/players")
                        .path("/squads")
                        .queryParam("team", teamApiId)
                        .build()
                )
                .retrieve()
                .body(JsonNode.class);
    }

    public JsonNode callExternalTeamTransfersApi(Long teamIdApiId) {
        return footballRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/transfers")
                        .queryParam("team", teamIdApiId)
                        .build()
                )
                .retrieve()
                .body(JsonNode.class);
    }

    public JsonNode callExternalLeagueTeamsApi(LeagueCode leagueCode) {
        return footballRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/teams")
                        .queryParam("league", leagueCode.getApiFootballLeagueId())
                        .queryParam("season", 2024) // 무료버전은 2022 ~ 2024 까지 요청이 가능하므로, 최신 데이터는 직접 ..
                        .build()
                )
                .retrieve()
                .body(JsonNode.class);
    }






}
