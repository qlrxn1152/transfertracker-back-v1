package com.dhoon.transfertracker.internal.fixture.service;
import com.dhoon.transfertracker.external.football.client.ApiFootballHttpClient;
import com.dhoon.transfertracker.external.football.client.StatisticsJson;
import com.dhoon.transfertracker.internal.fixture.dto.record.ApiFootballFixturePlayerStatisticsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import java.util.*;
@Service
@RequiredArgsConstructor
public class FixturePlayerStatisticsSyncService {
    private final ApiFootballHttpClient apiFootballHttpClient;
    private final FixturePlayerStatisticsSyncTxService txService;
    public void syncFixturePlayerStatistics(Long fixtureApiId) {
        JsonNode response = StatisticsJson.response(apiFootballHttpClient.callExternalFixturePlayerStatisticsApi(fixtureApiId));
        if (response.size() != 2) return;
        List<ApiFootballFixturePlayerStatisticsResponse> records = new ArrayList<>();
        Set<Long> teams = new HashSet<>();
        for (JsonNode node : response) {
            Long teamId = StatisticsJson.id(node.path("team").path("id"));
            if (!teams.add(teamId)) throw new IllegalArgumentException("Duplicate team");
            JsonNode players = node.path("players");
            if (!players.isArray() || players.isEmpty()) return;
            int before = records.size();
            for (JsonNode p : players) {
                JsonNode stats = p.path("statistics");
                if (!stats.isArray() || stats.size() != 1) return;
                records.add(ApiFootballFixturePlayerStatisticsResponse.from(node.path("team"), p.path("player"), stats.get(0)));
            }
            if (records.subList(before, records.size()).stream().noneMatch(r -> r.minutes() != null && r.minutes() > 0)) return;
        }
        txService.syncFixturePlayerStatistics(fixtureApiId, records);
    }
}
