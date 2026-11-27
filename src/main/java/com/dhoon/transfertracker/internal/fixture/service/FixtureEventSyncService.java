package com.dhoon.transfertracker.internal.fixture.service;

import com.dhoon.transfertracker.external.football.client.ApiFootballHttpClient;
import com.dhoon.transfertracker.internal.fixture.dto.record.ApiFootballFixtureEventResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FixtureEventSyncService {

    private final ApiFootballHttpClient apiFootballHttpClient;
    private final FixtureEventSyncTxService txService;

    public void syncFixtureEvents(Long fixtureApiId) {
        JsonNode root = apiFootballHttpClient.callExternalFixtureEventApi(fixtureApiId);

        if (root == null
                || !root.path("errors").isEmpty()
                || !root.path("response").isArray()) {

            throw new IllegalStateException(
                    "경기 이벤트 API 응답이 올바르지 않습니다."
            );
        }

        List<ApiFootballFixtureEventResponse> events = new ArrayList<>();

        for (JsonNode event : root.get("response")) {
            events.add(ApiFootballFixtureEventResponse.from(event));
        }

        txService.syncFixtureEvents(fixtureApiId, events);
    }

    public void syncCompletedFixtureEvents() {

        List<Long> fixtureApiIds = txService.getFixturesToSyncEvents();

        for (Long fixtureApiId : fixtureApiIds) {
            syncFixtureEvents(fixtureApiId);
        }

    }

}
