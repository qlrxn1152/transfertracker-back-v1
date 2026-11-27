package com.dhoon.transfertracker.internal.fixture.service;

import com.dhoon.transfertracker.external.football.client.ApiFootballHttpClient;
import com.dhoon.transfertracker.external.football.dto.ApiFootballFixtureResponse;
import com.dhoon.transfertracker.internal.fixture.domain.Fixture;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FixtureSyncService {

    private final ApiFootballHttpClient apiFootballHttpClient;
    private final FixtureSyncTxService txService;


    // 리그의 해당 시즌에 속한 경기들
    public void syncLeagueFixtures(LeagueCode leagueCode, Integer season) {
        JsonNode root = apiFootballHttpClient.callExternalLeagueFixturesApi(leagueCode, season);

        List<ApiFootballFixtureResponse> fixtures = new ArrayList<>();

        for (JsonNode node : root.get("response")) {
            fixtures.add(ApiFootballFixtureResponse.from(node));
        }

        txService.syncFixtures(fixtures);
    }

    public void syncLiveFixtures() {
        JsonNode root = apiFootballHttpClient.callExternalLiveFixturesApi();

        List<ApiFootballFixtureResponse> fixtures = new ArrayList<>();

        for (JsonNode node : root.get("response")) {
            fixtures.add(ApiFootballFixtureResponse.from(node));
        }

        txService.updateFixture(fixtures);
    }


    public void syncFixtureCompletion() {
        List<Fixture> fixtures = txService.getFixturesToCheckCompletion();

        List<ApiFootballFixtureResponse> records = new ArrayList<>();

        fixtures.forEach(findFixture -> {
            JsonNode root = apiFootballHttpClient.callExternalFixtureApi(findFixture.getApiFootballId());
            JsonNode response = root.get("response");

            response.forEach(node -> records.add(ApiFootballFixtureResponse.from(node)));
        });

        txService.updateFixture(records);
    }




}
