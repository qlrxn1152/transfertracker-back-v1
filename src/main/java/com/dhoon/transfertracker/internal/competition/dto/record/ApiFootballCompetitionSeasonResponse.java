package com.dhoon.transfertracker.internal.competition.dto.record;

import tools.jackson.databind.JsonNode;

import java.time.LocalDate;

public record ApiFootballCompetitionSeasonResponse(
        Integer season,
        LocalDate startDate,
        LocalDate endDate,
        boolean currentSeason,
        boolean supportsEvents,
        boolean supportsLineups,
        boolean supportsFixtureStatistics,
        boolean supportsPlayerStatistics,
        boolean supportsStandings,
        boolean supportsPlayers,
        boolean supportsTopScorers,
        boolean supportsTopAssists,
        boolean supportsTopCards,
        boolean supportsInjuries
) {

    public static ApiFootballCompetitionSeasonResponse from(
            JsonNode node
    ) {

        JsonNode coverageNode = node.path("coverage");
        JsonNode fixturesNode = coverageNode.path("fixtures");

        if (!node.path("year").isIntegralNumber()
                || !coverageNode.isObject()
                || !fixturesNode.isObject()) {

            throw new IllegalArgumentException(
                    "유효하지 않은 대회 시즌 정보입니다."
            );
        }

        return new ApiFootballCompetitionSeasonResponse(
                node.path("year").asInt(),
                getLocalDate(node.path("start")),
                getLocalDate(node.path("end")),
                node.path("current").asBoolean(),

                fixturesNode.path("events").asBoolean(),
                fixturesNode.path("lineups").asBoolean(),
                fixturesNode.path("statistics_fixtures").asBoolean(),
                fixturesNode.path("statistics_players").asBoolean(),

                coverageNode.path("standings").asBoolean(),
                coverageNode.path("players").asBoolean(),
                coverageNode.path("top_scorers").asBoolean(),
                coverageNode.path("top_assists").asBoolean(),
                coverageNode.path("top_cards").asBoolean(),
                coverageNode.path("injuries").asBoolean()
        );
    }

    private static LocalDate getLocalDate(JsonNode node) {

        if (node.isMissingNode() || node.isNull()) {
            return null;
        }

        return LocalDate.parse(node.asText());
    }
}