package com.dhoon.transfertracker.internal.fixture.dto.record;

import com.dhoon.transfertracker.internal.fixture.domain.FixtureLineupPlayerStatus;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

public record ApiFootballFixtureLineupResponse(
        Long teamApiId,
        String formation,
        List<ApiFootballFixtureLineupPlayerResponse> players
) {

    public static ApiFootballFixtureLineupResponse from(JsonNode node) {

        Long teamApiId = getLong(node.path("team").path("id"));
        String formation = getString(node.path("formation"));

        if (teamApiId == null) {
            throw new IllegalArgumentException(
                    "필수 라인업 팀 정보가 누락되었습니다."
            );
        }

        JsonNode startXINode = node.path("startXI");
        JsonNode substitutesNode = node.path("substitutes");

        if (!startXINode.isArray() || !substitutesNode.isArray()) {
            throw new IllegalArgumentException(
                    "유효하지 않은 라인업 선수 목록입니다."
            );
        }

        List<ApiFootballFixtureLineupPlayerResponse> players =
                new ArrayList<>();

        for (JsonNode playerNode : startXINode) {
            players.add(ApiFootballFixtureLineupPlayerResponse.from(
                    playerNode,
                    FixtureLineupPlayerStatus.STARTER
            ));
        }

        for (JsonNode playerNode : substitutesNode) {
            players.add(ApiFootballFixtureLineupPlayerResponse.from(
                    playerNode,
                    FixtureLineupPlayerStatus.SUBSTITUTE
            ));
        }

        return new ApiFootballFixtureLineupResponse(
                teamApiId,
                formation,
                players
        );
    }

    private static Long getLong(JsonNode node) {
        return node.isMissingNode() || node.isNull()
                ? null
                : node.asLong();
    }

    private static String getString(JsonNode node) {
        return node.isMissingNode() || node.isNull()
                ? null
                : node.asText();
    }
}