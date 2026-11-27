package com.dhoon.transfertracker.internal.fixture.dto.record;
import com.dhoon.transfertracker.internal.fixture.domain.FixtureStatistics;
import java.math.BigDecimal;
import java.time.Instant;
public record FixtureStatisticsResponse(
        Long id,
        Long teamId,
        String teamName,
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
        BigDecimal expectedGoals,
        Instant updatedAt
) {
    public static FixtureStatisticsResponse from(FixtureStatistics e) {
        return new FixtureStatisticsResponse(
            e.getId(),
            e.getTeam().getId(),
            e.getTeam().getDisplayName(),
            e.getPassesAccurate(),
            e.getShotsInsideBox(),
            e.getShotsOutsideBox(),
            e.getBlockedShots(),
            e.getFreeKicks(),
            e.getShotsOnGoal(),
            e.getGoalkeeperSaves(),
            e.getShotsOffGoal(),
            e.getTotalPasses(),
            e.getTotalShots(),
            e.getYellowCards(),
            e.getCornerKicks(),
            e.getOffsides(),
            e.getFouls(),
            e.getRedCards(),
            e.getBallPossession(),
            e.getExpectedGoals(),
            e.getUpdatedAt()
        );
    }
}
