package com.dhoon.transfertracker.external.football.client;

import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.monitoring.ExternalApiMetrics;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

@Slf4j
@Component
public class ApiFootballHttpClient {

    private final RestClient footballRestClient;
    private final ObjectProvider<ExternalApiMetrics> externalApiMetricsProvider;

    public ApiFootballHttpClient(
            @Qualifier("footballRestClient") RestClient footballRestClient,
            ObjectProvider<ExternalApiMetrics> externalApiMetricsProvider
    ) {
        this.footballRestClient = footballRestClient;
        this.externalApiMetricsProvider = externalApiMetricsProvider;
    }

    public JsonNode callExternalPlayerApi(Long playerApiId) {
        return record("player_profile", () -> {
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
        });
    }

    public JsonNode callExternalTeamApi(Long teamApiID) {
        return record("team_profile", () -> {
            JsonNode node = footballRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/teams")
                        .queryParam("id", teamApiID)
                        .build()
                )
                .retrieve()
                .body(JsonNode.class);
            return node.get("response").get(0).get("team");
        });
    }

    public JsonNode callExternalPlayerTransferApi(Long playerApiId) {
        return record("player_transfers", () -> footballRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/transfers")
                        .queryParam("player", playerApiId)
                        .build()
                )
                .retrieve()
                .body(JsonNode.class));
    }

    public JsonNode callExternalTeamPlayersApi(Long teamApiId) {
        return record("team_players", () -> footballRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/players")
                        .path("/squads")
                        .queryParam("team", teamApiId)
                        .build()
                )
                .retrieve()
                .body(JsonNode.class));
    }

    public JsonNode callExternalTeamTransfersApi(Long teamIdApiId) {
        return record("team_transfers", () -> footballRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/transfers")
                        .queryParam("team", teamIdApiId)
                        .build()
                )
                .retrieve()
                .body(JsonNode.class));
    }

    public JsonNode callExternalLeagueTeamsApi(LeagueCode leagueCode) {
        return record("league_teams", () -> footballRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/teams")
                        .queryParam("league", leagueCode.getApiFootballLeagueId())
                        .queryParam("season", 2024) // 무료버전은 2022 ~ 2024 까지 요청이 가능하므로, 최신 데이터는 직접 ..
                        .build()
                )
                .retrieve()
                .body(JsonNode.class));
    }

    private <T> T record(String operation, java.util.function.Supplier<T> call) {
        ExternalApiMetrics metrics = externalApiMetricsProvider.getIfAvailable();
        return metrics == null ? call.get() : metrics.record("api_football", operation, call);
    }






}
