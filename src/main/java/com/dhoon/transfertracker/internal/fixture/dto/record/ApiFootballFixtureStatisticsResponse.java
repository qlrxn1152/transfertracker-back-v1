package com.dhoon.transfertracker.internal.fixture.dto.record;
import tools.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.*;
import com.dhoon.transfertracker.external.football.client.StatisticsJson;
public record ApiFootballFixtureStatisticsResponse(
        Long teamApiId,
        Integer passesAccurate,
        Integer shotsInsideBox,
        Integer shotsOutsideBox,
        Integer blockedShots,
        Integer freeKicks,
        Integer shotsOnGoal,
        Integer goalkeeperSaves,
        Integer shotsOffGoal,
        Integer totalPasses,
        Integer totalShots,
        Integer yellowCards,
        Integer cornerKicks,
        Integer offsides,
        Integer fouls,
        Integer redCards,
        Integer ballPossession,
        BigDecimal expectedGoals
) {
    public static ApiFootballFixtureStatisticsResponse from(JsonNode node) {
        Map<String, JsonNode> values = new HashMap<>();
        JsonNode stats = node.path("statistics");
        if (!stats.isArray() || stats.isEmpty()) throw new IllegalArgumentException("Empty team statistics");
        for (JsonNode stat : stats) values.put(stat.path("type").asText(), stat.path("value"));
        return new ApiFootballFixtureStatisticsResponse(
            StatisticsJson.id(node.path("team").path("id")),
            StatisticsJson.integer(values.get("Passes accurate")),
            StatisticsJson.integer(values.get("Shots insidebox")),
            StatisticsJson.integer(values.get("Shots outsidebox")),
            StatisticsJson.integer(values.get("Blocked Shots")),
            StatisticsJson.integer(values.get("Free Kicks")),
            StatisticsJson.integer(values.get("Shots on Goal")),
            StatisticsJson.integer(values.get("Goalkeeper Saves")),
            StatisticsJson.integer(values.get("Shots off Goal")),
            StatisticsJson.integer(values.get("Total passes")),
            StatisticsJson.integer(values.get("Total Shots")),
            StatisticsJson.integer(values.get("Yellow Cards")),
            StatisticsJson.integer(values.get("Corner Kicks")),
            StatisticsJson.integer(values.get("Offsides")),
            StatisticsJson.integer(values.get("Fouls")),
            StatisticsJson.integer(values.get("Red Cards")),
            StatisticsJson.integer(values.get("Ball Possession")),
            StatisticsJson.decimal(values.get("expected_goals"))
        );
    }
}
