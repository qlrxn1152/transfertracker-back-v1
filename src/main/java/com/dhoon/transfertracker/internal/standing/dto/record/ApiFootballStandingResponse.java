package com.dhoon.transfertracker.internal.standing.dto.record;

import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.time.OffsetDateTime;

public record ApiFootballStandingResponse(
        Long teamApiId,
        Integer ranking,
        Integer points,
        Integer goalsDiff,
        String groupName,
        String form,
        String status,
        String description,
        StandingRecordResponse all,
        StandingRecordResponse home,
        StandingRecordResponse away,
        Instant sourceUpdatedAt
) {

    public static ApiFootballStandingResponse from(JsonNode node) {

        return new ApiFootballStandingResponse(
                getLong(node.path("team").path("id")),
                getInteger(node.path("rank")),
                getInteger(node.path("points")),
                getInteger(node.path("goalsDiff")),
                getString(node.path("group")),
                getString(node.path("form")),
                getString(node.path("status")),
                getString(node.path("description")),
                StandingRecordResponse.from(node.path("all")),
                StandingRecordResponse.from(node.path("home")),
                StandingRecordResponse.from(node.path("away")),
                getInstant(node.path("update"))
        );
    }

    public record StandingRecordResponse(
            Integer played,
            Integer win,
            Integer draw,
            Integer lose,
            Integer goalsFor,
            Integer goalsAgainst
    ) {

        public static StandingRecordResponse from(JsonNode node) {

            JsonNode goalsNode = node.path("goals");

            return new StandingRecordResponse(
                    getInteger(node.path("played")),
                    getInteger(node.path("win")),
                    getInteger(node.path("draw")),
                    getInteger(node.path("lose")),
                    getInteger(goalsNode.path("for")),
                    getInteger(goalsNode.path("against"))
            );
        }
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

    private static Instant getInstant(JsonNode node) {

        String value = getString(node);

        return value == null
                ? null
                : OffsetDateTime.parse(value).toInstant();
    }
}