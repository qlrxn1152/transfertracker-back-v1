package com.dhoon.transfertracker.internal.competition.dto.record;

import com.dhoon.transfertracker.internal.competition.domain.CompetitionSeason;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;

import java.time.Instant;
import java.time.LocalDate;

public record CompetitionSeasonResponse(
        Long competitionSeasonId,
        LeagueCode leagueCode,
        Long leagueApiId,
        Integer season,
        LocalDate startDate,
        LocalDate endDate,
        boolean currentSeason,
        String logoUrl,
        CoverageResponse coverage,
        Instant updatedAt
) {

    public static CompetitionSeasonResponse from(
            CompetitionSeason competitionSeason
    ) {

        return new CompetitionSeasonResponse(
                competitionSeason.getId(),
                competitionSeason.getLeagueCode(),
                competitionSeason.getLeagueCode()
                        .getApiFootballLeagueId(),
                competitionSeason.getSeason(),
                competitionSeason.getStartDate(),
                competitionSeason.getEndDate(),
                competitionSeason.isCurrentSeason(),
                "https://media.api-sports.io/football/leagues/"
                        + competitionSeason.getLeagueCode()
                                .getApiFootballLeagueId()
                        + ".png",
                CoverageResponse.from(competitionSeason),
                competitionSeason.getUpdatedAt()
        );
    }

    public record CoverageResponse(
            boolean events,
            boolean lineups,
            boolean fixtureStatistics,
            boolean playerStatistics,
            boolean standings,
            boolean players,
            boolean topScorers,
            boolean topAssists,
            boolean topCards,
            boolean injuries
    ) {

        public static CoverageResponse from(
                CompetitionSeason competitionSeason
        ) {

            return new CoverageResponse(
                    competitionSeason.isSupportsEvents(),
                    competitionSeason.isSupportsLineups(),
                    competitionSeason.isSupportsFixtureStatistics(),
                    competitionSeason.isSupportsPlayerStatistics(),
                    competitionSeason.isSupportsStandings(),
                    competitionSeason.isSupportsPlayers(),
                    competitionSeason.isSupportsTopScorers(),
                    competitionSeason.isSupportsTopAssists(),
                    competitionSeason.isSupportsTopCards(),
                    competitionSeason.isSupportsInjuries()
            );
        }
    }
}