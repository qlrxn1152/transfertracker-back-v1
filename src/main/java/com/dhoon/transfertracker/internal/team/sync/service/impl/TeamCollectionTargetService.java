package com.dhoon.transfertracker.internal.team.sync.service.impl;

import com.dhoon.transfertracker.external.football.client.ApiFootballHttpClient;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.sync.dto.record.TeamCollectionTarget;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamCollectionTargetService {

    private final ApiFootballHttpClient apiFootballHttpClient;

    public List<TeamCollectionTarget> getCollectionTargets(
            Long leagueApiId
    ) {

        Integer season = resolveCurrentSeason(leagueApiId);

        JsonNode root = apiFootballHttpClient
                .callExternalLeagueTeamsApi(
                        leagueApiId,
                        season
                );

        JsonNode responses = validateResponse(root);

        if (responses.isEmpty()) {
            throw new IllegalStateException(
                    "참가 팀 정보가 없습니다. leagueApiId="
                            + leagueApiId
            );
        }

        LeagueCode domesticLeague =
                resolveDomesticLeague(leagueApiId);

        List<TeamCollectionTarget> targets =
                new ArrayList<>();

        for (JsonNode response : responses) {

            JsonNode teamData = response.path("team");

            validateTeamData(teamData);

            targets.add(
                    new TeamCollectionTarget(
                            teamData.path("id").asLong(),
                            teamData.path("name").asText(),
                            teamData.path("national").asBoolean(),
                            domesticLeague,
                            teamData
                    )
            );
        }

        log.info(
                "대회 참가 팀 조회 완료. leagueApiId={}, season={}, count={}",
                leagueApiId,
                season,
                targets.size()
        );

        return targets;
    }

    private Integer resolveCurrentSeason(Long leagueApiId) {

        JsonNode root = apiFootballHttpClient
                .callExternalCompetitionSeasonApi(leagueApiId);

        JsonNode responses = validateResponse(root);

        Integer currentSeason = null;

        for (JsonNode response : responses) {

            if (response.path("league").path("id").asLong()
                    != leagueApiId) {
                continue;
            }

            JsonNode seasons = response.path("seasons");

            if (!seasons.isArray()) {
                continue;
            }

            for (JsonNode season : seasons) {

                if (!season.path("current").asBoolean()
                        || !season.path("year").isIntegralNumber()) {
                    continue;
                }

                int year = season.path("year").asInt();

                if (currentSeason == null
                        || year > currentSeason) {

                    currentSeason = year;
                }
            }
        }

        if (currentSeason == null) {
            throw new IllegalStateException(
                    "현재 시즌 정보가 없습니다. leagueApiId="
                            + leagueApiId
            );
        }

        return currentSeason;
    }

    private LeagueCode resolveDomesticLeague(Long leagueApiId) {

        return Arrays.stream(LeagueCode.values())
                .filter(LeagueCode::isDomesticLeague)
                .filter(leagueCode ->
                        leagueCode.getApiFootballLeagueId()
                                == leagueApiId
                )
                .findFirst()
                .orElse(null);
    }

    private JsonNode validateResponse(JsonNode root) {

        if (root == null
                || !root.isObject()
                || !root.has("errors")
                || !root.path("errors").isEmpty()
                || !root.path("response").isArray()) {

            throw new IllegalStateException(
                    "유효하지 않은 API-Football 응답입니다."
            );
        }

        return root.path("response");
    }

    private void validateTeamData(JsonNode teamData) {

        if (!teamData.path("id").isIntegralNumber()
                || !teamData.path("id").canConvertToLong()
                || teamData.path("id").asLong() <= 0
                || !teamData.path("name").isTextual()
                || teamData.path("name").asText().isBlank()
                || !teamData.path("national").isBoolean()) {

            throw new IllegalArgumentException(
                    "유효하지 않은 팀 데이터입니다."
            );
        }
    }
}