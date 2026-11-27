package com.dhoon.transfertracker.internal.fixture.dto.record;

import com.dhoon.transfertracker.internal.fixture.domain.Fixture;
import com.dhoon.transfertracker.internal.fixture.domain.FixtureStatus;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.Team;

import java.time.Instant;

public record FixtureResponse(
        Long fixtureId,
        Long apiFootballId,
        LeagueCode leagueCode,
        Integer season,
        String round,
        TeamResponse homeTeam,
        TeamResponse awayTeam,
        Instant kickoffAt,
        FixtureStatus status,
        Integer elapsed,
        Integer extra,
        Integer homeScore,
        Integer awayScore,
        String venueName,
        Instant updatedAt
) {

    public static FixtureResponse from(Fixture fixture) {
        return new FixtureResponse(
                fixture.getId(),
                fixture.getApiFootballId(),
                fixture.getLeagueCode(),
                fixture.getSeason(),
                fixture.getRound(),
                TeamResponse.from(fixture.getHomeTeam()),
                TeamResponse.from(fixture.getAwayTeam()),
                fixture.getKickoffAt(),
                fixture.getStatus(),
                fixture.getElapsed(),
                fixture.getExtra(),
                fixture.getHomeScore(),
                fixture.getAwayScore(),
                fixture.getVenueName(),
                fixture.getUpdatedAt()
        );
    }

    public record TeamResponse(
            Long teamId,
            Long apiFootballId,
            String teamName
    ) {
        public static TeamResponse from(Team team) {
            return new TeamResponse(
                    team.getId(),
                    team.getApiFootballId(),
                    team.getDisplayName()
            );
        }
    }

}