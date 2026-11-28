package com.dhoon.transfertracker.internal.competition.domain;

import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "competition_season",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_competition_season_league_season",
                        columnNames = {
                                "league_code",
                                "season"
                        }
                )
        }
)
public class CompetitionSeason {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "competition_season_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "league_code", nullable = false)
    private LeagueCode leagueCode;

    @Column(name = "season", nullable = false)
    private Integer season;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "current_season", nullable = false)
    private boolean currentSeason;

    @Column(name = "supports_events", nullable = false)
    private boolean supportsEvents;

    @Column(name = "supports_lineups", nullable = false)
    private boolean supportsLineups;

    @Column(name = "supports_fixture_statistics", nullable = false)
    private boolean supportsFixtureStatistics;

    @Column(name = "supports_player_statistics", nullable = false)
    private boolean supportsPlayerStatistics;

    @Column(name = "supports_standings", nullable = false)
    private boolean supportsStandings;

    @Column(name = "supports_players", nullable = false)
    private boolean supportsPlayers;

    @Column(name = "supports_top_scorers", nullable = false)
    private boolean supportsTopScorers;

    @Column(name = "supports_top_assists", nullable = false)
    private boolean supportsTopAssists;

    @Column(name = "supports_top_cards", nullable = false)
    private boolean supportsTopCards;

    @Column(name = "supports_injuries", nullable = false)
    private boolean supportsInjuries;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static CompetitionSeason createCompetitionSeason(
            LeagueCode leagueCode,
            Integer season
    ) {
        return new CompetitionSeason(leagueCode, season);
    }

    private CompetitionSeason(
            LeagueCode leagueCode,
            Integer season
    ) {
        this.leagueCode = leagueCode;
        this.season = season;
        this.updatedAt = Instant.now();
    }

    public void updateSeason(
            LocalDate startDate,
            LocalDate endDate,
            boolean currentSeason
    ) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.currentSeason = currentSeason;
        this.updatedAt = Instant.now();
    }

    public void updateCoverage(
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
        this.supportsEvents = supportsEvents;
        this.supportsLineups = supportsLineups;
        this.supportsFixtureStatistics = supportsFixtureStatistics;
        this.supportsPlayerStatistics = supportsPlayerStatistics;
        this.supportsStandings = supportsStandings;
        this.supportsPlayers = supportsPlayers;
        this.supportsTopScorers = supportsTopScorers;
        this.supportsTopAssists = supportsTopAssists;
        this.supportsTopCards = supportsTopCards;
        this.supportsInjuries = supportsInjuries;

        this.updatedAt = Instant.now();
    }
}