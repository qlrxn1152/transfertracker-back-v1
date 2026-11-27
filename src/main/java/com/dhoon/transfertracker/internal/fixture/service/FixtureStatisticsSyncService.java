package com.dhoon.transfertracker.internal.fixture.service;
import com.dhoon.transfertracker.external.football.client.ApiFootballHttpClient;
import com.dhoon.transfertracker.external.football.client.StatisticsJson;
import com.dhoon.transfertracker.internal.fixture.dto.record.ApiFootballFixtureStatisticsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import java.util.*;
@Service
@RequiredArgsConstructor
public class FixtureStatisticsSyncService {
    private final ApiFootballHttpClient apiFootballHttpClient;
    private final FixtureStatisticsSyncTxService txService;
    public void syncFixtureStatistics(Long fixtureApiId) {
        JsonNode response = StatisticsJson.response(apiFootballHttpClient.callExternalFixtureStatisticsApi(fixtureApiId));
        if (response.size() != 2) return;
        List<ApiFootballFixtureStatisticsResponse> records = new ArrayList<>();
        Set<Long> teams = new HashSet<>();
        for (JsonNode node : response) {
            Long teamId = StatisticsJson.id(node.path("team").path("id"));
            if (!teams.add(teamId)) throw new IllegalArgumentException("Duplicate team");
            ApiFootballFixtureStatisticsResponse r = ApiFootballFixtureStatisticsResponse.from(node);
            if (r.totalShots() == null && r.ballPossession() == null && r.totalPasses() == null) return;
            records.add(r);
        }
        txService.syncFixtureStatistics(fixtureApiId, records);
    }
}
