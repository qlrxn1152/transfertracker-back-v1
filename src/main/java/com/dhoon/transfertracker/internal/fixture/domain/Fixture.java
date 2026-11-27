package com.dhoon.transfertracker.internal.fixture.domain;

import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.Team;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_fixture_api_football_id", columnNames = "api_football_id")
        }
)
public class Fixture {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fixture_id")
    private Long id;

    @Column(name = "api_football_id", nullable = false)
    private Long apiFootballId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_team_id", nullable = false)
    private Team homeTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "away_team_id", nullable = false)
    private Team awayTeam;

    @Enumerated(EnumType.STRING)
    @Column(name = "league_code", nullable = false)
    private LeagueCode leagueCode;

    @Column(name = "season", nullable = false)
    private Integer season;

    @Column(name = "round", nullable = false)
    private String round;

    @Column(name = "kickoff_at", nullable = false)
    private Instant kickoffAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FixtureStatus status;

    @Column(name = "elapsed")
    private Integer elapsed; // 경기 시작하고 몇분 지났는지

    @Column(name = "extra")
    private Integer extra; // 추가시간

    @Column(name = "home_score")
    private Integer homeScore;

    @Column(name = "away_score")
    private Integer awayScore;

    @Column(name = "venue_name")
    private String venueName;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "event_sync_completed", nullable = false)
    private boolean eventSyncCompleted = false;

    @Column(name = "lineup_sync_completed", nullable = false)
    private boolean lineupSyncCompleted = false;

    public void completeEventSync() {
        this.eventSyncCompleted = true;
    }

    public void completeLineupSync() {
        this.lineupSyncCompleted = true;
    }

    public static Fixture createFixture(
            Long apiFootballId,
            Team homeTeam,
            Team awayTeam,
            LeagueCode leagueCode,
            Integer season,
            String round,
            Instant kickoffAt,
            FixtureStatus status,
            Integer elapsed,
            Integer extra,
            Integer homeScore,
            Integer awayScore,
            String venueName
    ) {
        Fixture fixture = new Fixture(
                apiFootballId,
                homeTeam,
                awayTeam,
                leagueCode,
                season,
                round
        );

        fixture.updateFixture(
                kickoffAt,
                status,
                elapsed,
                extra,
                homeScore,
                awayScore,
                venueName
        );

        return fixture;
    }

    public void updateFixture(Instant kickoffAt, FixtureStatus status, Integer elapsed, Integer extra, Integer homeScore, Integer awayScore, String venueName) {
        this.kickoffAt = kickoffAt;
        this.status = status;
        this.elapsed = elapsed;
        this.extra = extra;
        this.homeScore = homeScore;
        this.awayScore = awayScore;
        this.venueName = venueName;
        this.updatedAt = Instant.now();
    }







    // 처음 경기 생성
    private Fixture(Long apiFootballId, Team homeTeam, Team awayTeam, LeagueCode leagueCode, Integer season, String round) {
        this.apiFootballId = apiFootballId;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.leagueCode = leagueCode;
        this.season = season;
        this.round = round;
    }

}
