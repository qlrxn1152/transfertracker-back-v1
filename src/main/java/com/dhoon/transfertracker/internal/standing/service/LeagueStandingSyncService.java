package com.dhoon.transfertracker.internal.standing.service;

import com.dhoon.transfertracker.external.football.client.ApiFootballHttpClient;
import com.dhoon.transfertracker.internal.standing.dto.record.ApiFootballStandingResponse;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeagueStandingSyncService {

    private final ApiFootballHttpClient apiFootballHttpClient;
    private final LeagueStandingSyncTxService leagueStandingSyncTxService;

    public void syncLeagueStandings(
            LeagueCode leagueCode,
            Integer season
    ) {

        JsonNode root = apiFootballHttpClient
                .callExternalLeagueStandingApi(
                        leagueCode.getApiFootballLeagueId(),
                        season
                );

        if (root == null
                || !root.path("errors").isEmpty()
                || !root.path("response").isArray()) {

            throw new IllegalStateException(
                    "유효하지 않은 리그 순위 API 응답입니다."
            );
        }

        List<ApiFootballStandingResponse> standings =
                new ArrayList<>();

        for (JsonNode responseNode : root.path("response")) {

            JsonNode leagueNode = responseNode.path("league");

            if (leagueNode.path("id").asLong()
                    != leagueCode.getApiFootballLeagueId()
                    || leagueNode.path("season").asInt() != season) {

                throw new IllegalStateException(
                        "요청한 리그 또는 시즌과 일치하지 않습니다."
                );
            }

            JsonNode standingsNode = leagueNode.path("standings");

            if (!standingsNode.isArray()) {
                throw new IllegalStateException(
                        "유효하지 않은 리그 순위 목록입니다."
                );
            }

            for (JsonNode groupNode : standingsNode) {

                if (!groupNode.isArray()) {
                    throw new IllegalStateException(
                            "유효하지 않은 리그 순위 그룹입니다."
                    );
                }

                for (JsonNode standingNode : groupNode) {

                    ApiFootballStandingResponse standing =
                            ApiFootballStandingResponse.from(
                                    standingNode
                            );

                    standings.add(standing);
                }
            }
        }

        if (standings.isEmpty()) {
            log.debug(
                    "리그 순위 데이터 없음. leagueCode={}, season={}",
                    leagueCode,
                    season
            );
            return;
        }

        leagueStandingSyncTxService.syncLeagueStandings(
                leagueCode,
                season,
                standings
        );
    }

    public void syncAllLeagueStandings() {

        LocalDate today = LocalDate.now(
                ZoneId.of("Asia/Seoul")
        );

        for (LeagueCode leagueCode : LeagueCode.values()) {

            try {
                int season = leagueCode.resolveSeason(today);

                syncLeagueStandings(leagueCode, season);

            } catch (Exception e) {
                log.error(
                        "리그 순위 동기화 실패. leagueCode={}",
                        leagueCode,
                        e
                );
            }
        }
    }
}