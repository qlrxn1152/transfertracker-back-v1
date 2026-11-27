package com.dhoon.transfertracker.internal.standing.domain;

import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.Team;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "league_standing",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_league_standing_league_season_group_team",
                        columnNames = {
                                "league_code",
                                "season",
                                "group_name",
                                "team_id"
                        }
                )
        }
)
public class LeagueStanding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "league_standing_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Enumerated(EnumType.STRING)
    @Column(name = "league_code", nullable = false)
    private LeagueCode leagueCode;

    @Column(nullable = false)
    private Integer season;

    @Column(name = "group_name", nullable = false)
    private String groupName;

    @Column(nullable = false)
    private Integer ranking;

    @Column(nullable = false)
    private Integer points;

    @Column(name = "goals_diff", nullable = false)
    private Integer goalsDiff;

    private String form;

    private String status;

    private String description;

    // 전체 경기 성적

    @Column(name = "all_played")
    private Integer allPlayed;

    @Column(name = "all_win")
    private Integer allWin;

    @Column(name = "all_draw")
    private Integer allDraw;

    @Column(name = "all_lose")
    private Integer allLose;

    @Column(name = "all_goals_for")
    private Integer allGoalsFor;

    @Column(name = "all_goals_against")
    private Integer allGoalsAgainst;

    // 홈 경기 성적

    @Column(name = "home_played")
    private Integer homePlayed;

    @Column(name = "home_win")
    private Integer homeWin;

    @Column(name = "home_draw")
    private Integer homeDraw;

    @Column(name = "home_lose")
    private Integer homeLose;

    @Column(name = "home_goals_for")
    private Integer homeGoalsFor;

    @Column(name = "home_goals_against")
    private Integer homeGoalsAgainst;

    // 원정 경기 성적

    @Column(name = "away_played")
    private Integer awayPlayed;

    @Column(name = "away_win")
    private Integer awayWin;

    @Column(name = "away_draw")
    private Integer awayDraw;

    @Column(name = "away_lose")
    private Integer awayLose;

    @Column(name = "away_goals_for")
    private Integer awayGoalsFor;

    @Column(name = "away_goals_against")
    private Integer awayGoalsAgainst;

    @Column(name = "source_updated_at")
    private Instant sourceUpdatedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static LeagueStanding createLeagueStanding(
            Team team,
            LeagueCode leagueCode,
            Integer season,
            String groupName
    ) {
        return new LeagueStanding(
                team,
                leagueCode,
                season,
                groupName
        );
    }

    private LeagueStanding(
            Team team,
            LeagueCode leagueCode,
            Integer season,
            String groupName
    ) {
        this.team = team;
        this.leagueCode = leagueCode;
        this.season = season;
        this.groupName = groupName;
    }

    public void updateStanding(
            Integer ranking,
            Integer points,
            Integer goalsDiff,
            String form,
            String status,
            String description,
            Instant sourceUpdatedAt
    ) {
        this.ranking = ranking;
        this.points = points;
        this.goalsDiff = goalsDiff;
        this.form = form;
        this.status = status;
        this.description = description;
        this.sourceUpdatedAt = sourceUpdatedAt;
        this.updatedAt = Instant.now();
    }

    public void updateAllRecord(
            Integer played,
            Integer win,
            Integer draw,
            Integer lose,
            Integer goalsFor,
            Integer goalsAgainst
    ) {
        this.allPlayed = played;
        this.allWin = win;
        this.allDraw = draw;
        this.allLose = lose;
        this.allGoalsFor = goalsFor;
        this.allGoalsAgainst = goalsAgainst;
    }

    public void updateHomeRecord(
            Integer played,
            Integer win,
            Integer draw,
            Integer lose,
            Integer goalsFor,
            Integer goalsAgainst
    ) {
        this.homePlayed = played;
        this.homeWin = win;
        this.homeDraw = draw;
        this.homeLose = lose;
        this.homeGoalsFor = goalsFor;
        this.homeGoalsAgainst = goalsAgainst;
    }

    public void updateAwayRecord(
            Integer played,
            Integer win,
            Integer draw,
            Integer lose,
            Integer goalsFor,
            Integer goalsAgainst
    ) {
        this.awayPlayed = played;
        this.awayWin = win;
        this.awayDraw = draw;
        this.awayLose = lose;
        this.awayGoalsFor = goalsFor;
        this.awayGoalsAgainst = goalsAgainst;
    }
}