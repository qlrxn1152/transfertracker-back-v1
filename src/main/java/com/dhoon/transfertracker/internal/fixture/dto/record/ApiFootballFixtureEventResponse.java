package com.dhoon.transfertracker.internal.fixture.dto.record;

import tools.jackson.databind.JsonNode;

public record ApiFootballFixtureEventResponse(
        Long teamApiId,
        String teamName,

        Long playerApiId,
        String playerName,
        Long relatedPlayerApiId,
        String relatedPlayerName,
        Integer elapsed,
        Integer extra,
        String type,
        String detail,
        String comments
) {

    public static ApiFootballFixtureEventResponse from(JsonNode node) {

        JsonNode team = node.path("team");
        JsonNode player = node.path("player");
        JsonNode relatedPlayer = node.path("assist");
        JsonNode time = node.path("time");

        Long teamApiId = getNullableLong(team.path("id"));
        Integer elapsed = getNullableInt(time.path("elapsed"));
        String type = getNullableString(node.path("type"));
        String detail = getNullableString(node.path("detail"));

        if (teamApiId == null || elapsed == null
                || type == null || detail == null) {
            throw new IllegalArgumentException(
                    "경기 이벤트 필수 데이터가 누락되었습니다."
            );
        }

        return new ApiFootballFixtureEventResponse(
                teamApiId,
                getNullableString(team.get("name")),

                getNullableLong(player.path("id")),
                getNullableString(player.path("name")),

                getNullableLong(relatedPlayer.path("id")),
                getNullableString(relatedPlayer.path("name")),

                elapsed,
                getNullableInt(time.path("extra")),

                type,
                detail,
                getNullableString(node.path("comments"))
        );
    }

    private static Integer getNullableInt(JsonNode node) {
        return isNullOrMissing(node) ? null : node.asInt();
    }

    private static Long getNullableLong(JsonNode node) {
        return isNullOrMissing(node) ? null : node.asLong();
    }

    private static String getNullableString(JsonNode node) {
        return isNullOrMissing(node) ? null : node.asString();
    }

    private static boolean isNullOrMissing(JsonNode node) {
        return node == null || node.isNull() || node.isMissingNode();
    }
}