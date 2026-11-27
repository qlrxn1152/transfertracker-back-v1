package com.dhoon.transfertracker.internal.fixture.service;

import com.dhoon.transfertracker.external.football.client.ApiFootballHttpClient;
import com.dhoon.transfertracker.internal.fixture.dto.record.ApiFootballFixtureEventResponse;
import com.dhoon.transfertracker.internal.fixture.dto.record.ApiFootballFixtureResponse;
import com.dhoon.transfertracker.internal.fixture.domain.Fixture;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FixtureSyncService {

    private final ApiFootballHttpClient apiFootballHttpClient;
    private final FixtureSyncTxService fixtureSyncTxService;
    private final FixtureEventSyncTxService eventSyncTxService;


    // 리그의 해당 시즌에 속한 경기들
    public void syncLeagueFixtures(LeagueCode leagueCode, Integer season) {
        JsonNode root = apiFootballHttpClient.callExternalLeagueFixturesApi(leagueCode, season);

        List<ApiFootballFixtureResponse> fixtures = new ArrayList<>();

        for (JsonNode node : root.get("response")) {
            fixtures.add(ApiFootballFixtureResponse.from(node));
        }

        fixtureSyncTxService.syncFixtures(fixtures);
    }

    public void syncLiveFixtures() {
        JsonNode root = apiFootballHttpClient.callExternalLiveFixturesApi();

        List<ApiFootballFixtureResponse> fixtures = new ArrayList<>();

        Map<Long, List<ApiFootballFixtureEventResponse>> eventsByFixtureId =
                new HashMap<>();

        for (JsonNode node : root.get("response")) {

            Long leagueApiId = node.path("league").path("id").asLong();

            // 지원하지 않는 리그는 건너뛴다.
            if (!LeagueCode.isSupported(leagueApiId)) {
                continue;
            }

            ApiFootballFixtureResponse fixture =
                    ApiFootballFixtureResponse.from(node);

            fixtures.add(fixture);

            JsonNode eventsNode = node.get("events");

            if (eventsNode == null || !eventsNode.isArray()) {
                continue;
            }

            List<ApiFootballFixtureEventResponse> events = new ArrayList<>();

            for (JsonNode eventNode : eventsNode) {
                events.add(ApiFootballFixtureEventResponse.from(eventNode));
            }

            eventsByFixtureId.put(fixture.apiFootballId(), events);
        }

        fixtureSyncTxService.updateFixture(fixtures);

        eventsByFixtureId.forEach(eventSyncTxService::syncFixtureEvents);
    }

    public void syncFixtureCompletion() {
        List<Fixture> fixtures = fixtureSyncTxService.getFixturesToCheckCompletion();

        List<ApiFootballFixtureResponse> records = new ArrayList<>();

        fixtures.forEach(findFixture -> {
            JsonNode root = apiFootballHttpClient.callExternalFixtureApi(findFixture.getApiFootballId());
            JsonNode response = root.get("response");

            response.forEach(node -> records.add(ApiFootballFixtureResponse.from(node)));
        });

        fixtureSyncTxService.updateFixture(records);
    }




}
