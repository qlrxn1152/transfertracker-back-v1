package com.dhoon.transfertracker.internal.competition.service;

import com.dhoon.transfertracker.external.football.client.ApiFootballHttpClient;
import com.dhoon.transfertracker.internal.competition.dto.record.ApiFootballCompetitionSeasonResponse;
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
public class CompetitionSeasonSyncService {

    private final ApiFootballHttpClient apiFootballHttpClient;
    private final CompetitionSeasonSyncTxService competitionSeasonSyncTxService;

    public void syncCompetitionSeasons(LeagueCode leagueCode) {

        JsonNode root = apiFootballHttpClient
                .callExternalCompetitionSeasonApi(
                        leagueCode.getApiFootballLeagueId()
                );

        if (root == null
                || !root.path("errors").isEmpty()
                || !root.path("response").isArray()) {

            throw new IllegalStateException(
                    "유효하지 않은 대회 시즌 API 응답입니다."
            );
        }

        List<ApiFootballCompetitionSeasonResponse> seasons =
                new ArrayList<>();

        for (JsonNode responseNode : root.path("response")) {

            JsonNode leagueNode = responseNode.path("league");

            if (leagueNode.path("id").asLong()
                    != leagueCode.getApiFootballLeagueId()) {

                throw new IllegalStateException(
                        "요청한 대회와 응답이 일치하지 않습니다."
                );
            }

            JsonNode seasonsNode = responseNode.path("seasons");

            if (!seasonsNode.isArray()) {
                throw new IllegalStateException(
                        "유효하지 않은 시즌 목록입니다."
                );
            }

            for (JsonNode seasonNode : seasonsNode) {

                seasons.add(
                        ApiFootballCompetitionSeasonResponse.from(
                                seasonNode
                        )
                );
            }
        }

        if (seasons.isEmpty()) {
            log.warn(
                    "대회 시즌 정보 없음. leagueCode={}",
                    leagueCode
            );
            return;
        }

        competitionSeasonSyncTxService.syncCompetitionSeasons(
                leagueCode,
                seasons
        );
    }

    public void syncAllCompetitionSeasons() {

        for (LeagueCode leagueCode : LeagueCode.values()) {

            try {
                syncCompetitionSeasons(leagueCode);

            } catch (Exception e) {

                log.error(
                        "대회 시즌 동기화 실패. leagueCode={}",
                        leagueCode,
                        e
                );
            }
        }
    }
}