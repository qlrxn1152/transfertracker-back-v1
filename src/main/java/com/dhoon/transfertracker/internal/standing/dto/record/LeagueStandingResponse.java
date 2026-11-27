package com.dhoon.transfertracker.internal.standing.dto.record;

import com.dhoon.transfertracker.internal.standing.domain.LeagueStanding;
import com.dhoon.transfertracker.internal.team.domain.Team;

import java.time.Instant;

public record LeagueStandingResponse(
        Long leagueStandingId,
        Integer ranking,
        TeamResponse team,
        Integer points,
        Integer goalsDiff,
        String form,
        String status,
        String description,
        RecordResponse all,
        RecordResponse home,
        RecordResponse away,
        Instant sourceUpdatedAt,
        Instant updatedAt
) {

    public static LeagueStandingResponse from(
            LeagueStanding standing
    ) {
        return new LeagueStandingResponse(
                standing.getId(),
                standing.getRanking(),
                TeamResponse.from(standing.getTeam()),
                standing.getPoints(),
                standing.getGoalsDiff(),
                standing.getForm(),
                standing.getStatus(),
                standing.getDescription(),

                new RecordResponse(
                        standing.getAllPlayed(),
                        standing.getAllWin(),
                        standing.getAllDraw(),
                        standing.getAllLose(),
                        standing.getAllGoalsFor(),
                        standing.getAllGoalsAgainst()
                ),

                new RecordResponse(
                        standing.getHomePlayed(),
                        standing.getHomeWin(),
                        standing.getHomeDraw(),
                        standing.getHomeLose(),
                        standing.getHomeGoalsFor(),
                        standing.getHomeGoalsAgainst()
                ),

                new RecordResponse(
                        standing.getAwayPlayed(),
                        standing.getAwayWin(),
                        standing.getAwayDraw(),
                        standing.getAwayLose(),
                        standing.getAwayGoalsFor(),
                        standing.getAwayGoalsAgainst()
                ),

                standing.getSourceUpdatedAt(),
                standing.getUpdatedAt()
        );
    }

    public record TeamResponse(
            Long teamId,
            Long apiFootballId,
            String teamName,
            String logoUrl
    ) {

        public static TeamResponse from(Team team) {
            return new TeamResponse(
                    team.getId(),
                    team.getApiFootballId(),
                    team.getDisplayName(),
                    "https://media.api-sports.io/football/teams/"
                            + team.getApiFootballId() + ".png"
            );
        }
    }

    public record RecordResponse(
            Integer played,
            Integer win,
            Integer draw,
            Integer lose,
            Integer goalsFor,
            Integer goalsAgainst
    ) {
    }
}