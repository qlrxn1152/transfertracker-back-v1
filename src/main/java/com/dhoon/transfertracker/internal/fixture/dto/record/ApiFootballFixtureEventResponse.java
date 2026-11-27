package com.dhoon.transfertracker.internal.fixture.dto.record;

import tools.jackson.databind.JsonNode;

public record ApiFootballFixtureEventResponse(
        Long teamApiId,
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

        JsonNode team = node.get("team");
        JsonNode player = node.get("player");
        JsonNode relatedPlayer = node.get("assist");
        JsonNode time = node.get("time");
        JsonNode type = node.get("type");
        JsonNode detail = node.get("detail");
        JsonNode comments = node.get("comments");


        return new ApiFootballFixtureEventResponse(
                team.get("id").asLong(),

                getNullableLong(player.get("id")),
                getNullableString(player.get("name")),

                getNullableLong(relatedPlayer.get("id")),
                getNullableString(relatedPlayer.get("name")),

                getNullableInt(time.get("elapsed")),
                getNullableInt(time.get("extra")),

                getNullableString(type),
                getNullableString(detail),
                getNullableString(comments)
        );
    }

    private static Integer getNullableInt(JsonNode node) {
        return node == null || node.isNull() ? null : node.asInt();
    }

    private static Long getNullableLong(JsonNode node) {
        return node == null || node.isNull() ? null : node.asLong();
    }

    private static String getNullableString(JsonNode node) {
        return node == null || node.isNull() ? null : node.asString();
    }
}