package com.dhoon.transfertracker.internal.fixture.dto.record;

import com.dhoon.transfertracker.internal.fixture.domain.FixtureLineupPlayerStatus;
import tools.jackson.databind.JsonNode;

public record ApiFootballFixtureLineupPlayerResponse(
        Long playerApiId,
        String playerName,
        Integer number,
        String position,
        String grid,
        FixtureLineupPlayerStatus status
) {

    public static ApiFootballFixtureLineupPlayerResponse from(
            JsonNode node,
            FixtureLineupPlayerStatus status
    ) {
        JsonNode playerNode = node.path("player");

        Long playerApiId = getLong(playerNode.path("id"));
        String playerName = getString(playerNode.path("name"));
        Integer number = getInteger(playerNode.path("number"));
        String position = getString(playerNode.path("pos"));
        String grid = getString(playerNode.path("grid"));

        if (playerApiId == null || playerName == null || status == null) {
            throw new IllegalArgumentException(
                    "필수 라인업 선수 정보가 누락되었습니다."
            );
        }

        return new ApiFootballFixtureLineupPlayerResponse(
                playerApiId,
                playerName,
                number,
                position,
                grid,
                status
        );
    }

    private static Long getLong(JsonNode node) {
        return node.isMissingNode() || node.isNull()
                ? null
                : node.asLong();
    }

    private static Integer getInteger(JsonNode node) {
        return node.isMissingNode() || node.isNull()
                ? null
                : node.asInt();
    }

    private static String getString(JsonNode node) {
        return node.isMissingNode() || node.isNull()
                ? null
                : node.asText();
    }
}