package com.dhoon.transfertracker.internal.fixture.service;

import com.dhoon.transfertracker.external.football.client.ApiFootballHttpClient;
import com.dhoon.transfertracker.external.football.dto.ApiFootballFixtureResponse;
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

    public void syncLeagueFixtures(LeagueCode leagueCode, Integer season) {
        JsonNode root = apiFootballHttpClient.callExternalLeagueFixturesApi(leagueCode, season);

        List<ApiFootballFixtureResponse> fixtures = new ArrayList<>();

        for (JsonNode node : root.get("response")) {
            fixtures.add(ApiFootballFixtureResponse.from(node));
        }

        txService.syncFixtures(fixtures);
    }
}
