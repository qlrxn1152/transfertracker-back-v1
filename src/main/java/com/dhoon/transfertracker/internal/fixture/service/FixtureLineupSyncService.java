package com.dhoon.transfertracker.internal.fixture.service;

import com.dhoon.transfertracker.external.football.client.ApiFootballHttpClient;
import com.dhoon.transfertracker.internal.fixture.dto.record.ApiFootballFixtureLineupResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FixtureLineupSyncService {

    private final ApiFootballHttpClient apiFootballHttpClient;
    private final FixtureLineupSyncTxService fixtureLineupSyncTxService;

    public void syncFixtureLineups(Long fixtureApiId) {

        JsonNode root = apiFootballHttpClient
                .callExternalFixtureLineupApi(fixtureApiId);

        if (root == null
                || !root.path("errors").isEmpty()
                || !root.path("response").isArray()) {

            throw new IllegalStateException(
                    "유효하지 않은 경기 라인업 API 응답입니다."
            );
        }

        JsonNode responseNode = root.path("response");

        if (responseNode.isEmpty()) {
            log.debug(
                    "경기 라인업 데이터 없음. fixtureApiId={}",
                    fixtureApiId
            );
            return;
        }

        List<ApiFootballFixtureLineupResponse> lineups =
                new ArrayList<>();

        for (JsonNode node : responseNode) {
            lineups.add(ApiFootballFixtureLineupResponse.from(node));
        }

        fixtureLineupSyncTxService.syncFixtureLineups(
                fixtureApiId,
                lineups
        );
    }

    public void syncUpcomingFixtureLineups() {

        List<Long> fixtureApiIds = fixtureLineupSyncTxService.getFixturesToSyncLineups();

        if (fixtureApiIds.isEmpty()) {
            log.debug("라인업 동기화 대상 경기 없음");
            return;
        }

        for (Long fixtureApiId : fixtureApiIds) {
            try {
                syncFixtureLineups(fixtureApiId);
            } catch (Exception e) {
                log.error(
                        "경기 라인업 동기화 실패. fixtureApiId={}",
                        fixtureApiId,
                        e
                );
            }
        }
    }
}