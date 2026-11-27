package com.dhoon.transfertracker.external.football.dto;

import tools.jackson.databind.JsonNode;

import java.time.Instant;

public record ApiFootballFixtureResponse(
        Long apiFootballId,
        Long homeTeamApiId,
        Long awayTeamApiId,
        Long leagueApiId,
        Integer season,
        String round,
        Instant kickoffAt,
        String status,
        Integer elapsed,
        Integer extra,
        Integer homeScore,
        Integer awayScore,
        String venueName
) {

    public static ApiFootballFixtureResponse from(JsonNode node) {

        JsonNode fixture = node.get("fixture");
        JsonNode league = node.get("league");
        JsonNode teams = node.get("teams");
        JsonNode goals = node.get("goals");

        JsonNode status = fixture.get("status");
        JsonNode venue = fixture.get("venue");

        return new ApiFootballFixtureResponse(
                fixture.get("id").asLong(),
                teams.get("home").get("id").asLong(),
                teams.get("away").get("id").asLong(),
                league.get("id").asLong(),
                league.get("season").asInt(),
                league.get("round").asString(),
                Instant.ofEpochSecond(fixture.get("timestamp").asLong()),
                status.get("short").asString(),

                getNullableInt(status.get("elapsed")),
                getNullableInt(status.get("extra")),
                getNullableInt(goals.get("home")),
                getNullableInt(goals.get("away")),
                getNullableString(venue.get("name"))
        );
    }

    private static Integer getNullableInt(JsonNode node) {
        return node == null || node.isNull() ? null : node.asInt();
    }

    private static String getNullableString(JsonNode node) {
        return node == null || node.isNull() ? null : node.asString();
    }
}