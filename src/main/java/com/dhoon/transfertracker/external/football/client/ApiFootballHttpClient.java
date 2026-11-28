package com.dhoon.transfertracker.external.football.client;

import com.dhoon.transfertracker.internal.player.domain.PlayerRankingType;
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

    public JsonNode callExternalLeagueTeamsApi(LeagueCode leagueCode, Integer season) {
        return record("league_teams", () -> footballRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/teams")
                        .queryParam("league", leagueCode.getApiFootballLeagueId())
                        .queryParam("season", season)
                        .build()
                )
                .retrieve()
                .body(JsonNode.class));
    }

    public JsonNode callExternalLeagueTeamsApi(
            Long leagueApiId,
            Integer season
    ) {
        return record("league_teams", () -> footballRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/teams")
                        .queryParam("league", leagueApiId)
                        .queryParam("season", season)
                        .build()
                )
                .retrieve()
                .body(JsonNode.class));
    }


    ///  ====================================================

    // 특정 리그의 특정 시즌 경기들 조회
    public JsonNode callExternalLeagueFixturesApi(LeagueCode leagueCode, Integer season) {
        return record("league_fixtures", () -> footballRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/fixtures")
                        .queryParam("league", leagueCode.getApiFootballLeagueId())
                        .queryParam("season", season)
                        .build()
                )
                .retrieve()
                .body(JsonNode.class)
        );
    }

    // 진행중인 경기들
    public JsonNode callExternalLiveFixturesApi() {
        return record("live_fixtures", () -> footballRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/fixtures")
                        .queryParam("live", "all")
                        .build()
                )
                .retrieve()
                .body(JsonNode.class));
    }

    // 경기 상세
    public JsonNode callExternalFixtureApi(Long fixtureApiId) {
        return record("fixture_detail", () -> footballRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/fixtures")
                        .queryParam("id", fixtureApiId)
                        .build()
                )
                .retrieve()
                .body(JsonNode.class));
    }

    public JsonNode callExternalFixtureEventApi(Long fixtureApiId) {
        return record("fixture_events", () -> footballRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/fixtures")
                        .path("/events")
                        .queryParam("fixture", fixtureApiId)
                        .build()
                )
                .retrieve()
                .body(JsonNode.class));
    }

    public JsonNode callExternalFixtureLineupApi(Long fixtureApiId) {
        return record("fixture_lineups", () -> footballRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/fixtures")
                        .path("/lineups")
                        .queryParam("fixture", fixtureApiId)
                        .build()
                )
                .retrieve()
                .body(JsonNode.class));
    }

    public JsonNode callExternalLeagueStandingApi(Long leagueApiId, Integer season) {
        return record("league_standings", () -> footballRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/standings")
                        .queryParam("league", leagueApiId)
                        .queryParam("season", season)
                        .build()
                )
                .retrieve()
                .body(JsonNode.class));
    }

    public JsonNode callExternalFixtureStatisticsApi(Long fixtureApiId) {
        return record("fixture_statistics", () -> footballRestClient.get().uri(b -> b.path("/fixtures/statistics").queryParam("fixture", fixtureApiId).build()).retrieve().body(JsonNode.class));
    }
    public JsonNode callExternalFixturePlayerStatisticsApi(Long fixtureApiId) {
        return record("fixture_player_statistics", () -> footballRestClient.get().uri(b -> b.path("/fixtures/players").queryParam("fixture", fixtureApiId).build()).retrieve().body(JsonNode.class));
    }
    public JsonNode callExternalPlayerSeasonStatisticsApi(Long playerApiId, Integer season) {
        return record("player_season_statistics", () -> footballRestClient.get().uri(b -> b.path("/players").queryParam("id", playerApiId).queryParam("season", season).build()).retrieve().body(JsonNode.class));
    }
    public JsonNode callExternalPlayerRankingApi(Long leagueApiId, Integer season, PlayerRankingType type) {
        String path = type == PlayerRankingType.GOALS ? "/players/topscorers" : "/players/topassists";
        return record("player_rankings", () -> footballRestClient.get().uri(b -> b.path(path).queryParam("league", leagueApiId).queryParam("season", season).build()).retrieve().body(JsonNode.class));
    }
    public JsonNode callExternalLeaguePlayerSeasonStatisticsApi(Long leagueApiId, Integer season, Integer page) {
        return record("league_player_season_statistics", () -> footballRestClient.get().uri(b -> b.path("/players").queryParam("league", leagueApiId).queryParam("season", season).queryParam("page", page).build()).retrieve().body(JsonNode.class));
    }

    public JsonNode callExternalCompetitionSeasonApi(
            Long leagueApiId
    ) {
        return record("competition_seasons", () -> footballRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/leagues")
                        .queryParam("id", leagueApiId)
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
