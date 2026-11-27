package com.dhoon.transfertracker.internal.fixture.domain;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.Instant;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.player.domain.Player;
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "fixture_player_statistics", uniqueConstraints = @UniqueConstraint(name = "uk_fixture_player_statistics_key", columnNames = {"fixture_id", "team_id", "player_id"}))
public class FixturePlayerStatistics {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fixture_player_statistics_id")
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fixture_id", nullable = false)
    private Fixture fixture;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;
    @Column(name = "minutes")
    private Integer minutes;
    @Column(name = "number")
    private Integer number;
    @Column(name = "position")
    private String position;
    @Column(name = "rating", precision = 12, scale = 6)
    private BigDecimal rating;
    @Column(name = "captain")
    private Boolean captain;
    @Column(name = "shots_total")
    private Integer shotsTotal;
    @Column(name = "shots_on")
    private Integer shotsOn;
    @Column(name = "goals")
    private Integer goals;
    @Column(name = "goals_conceded")
    private Integer goalsConceded;
    @Column(name = "assists")
    private Integer assists;
    @Column(name = "saves")
    private Integer saves;
    @Column(name = "passes_total")
    private Integer passesTotal;
    @Column(name = "passes_key")
    private Integer passesKey;
    @Column(name = "tackles_total")
    private Integer tacklesTotal;
    @Column(name = "blocks")
    private Integer blocks;
    @Column(name = "interceptions")
    private Integer interceptions;
    @Column(name = "duels_total")
    private Integer duelsTotal;
    @Column(name = "duels_won")
    private Integer duelsWon;
    @Column(name = "dribbles_attempts")
    private Integer dribblesAttempts;
    @Column(name = "dribbles_success")
    private Integer dribblesSuccess;
    @Column(name = "dribbled_past")
    private Integer dribbledPast;
    @Column(name = "fouls_drawn")
    private Integer foulsDrawn;
    @Column(name = "fouls_committed")
    private Integer foulsCommitted;
    @Column(name = "yellow_cards")
    private Integer yellowCards;
    @Column(name = "red_cards")
    private Integer redCards;
    @Column(name = "penalty_won")
    private Integer penaltyWon;
    @Column(name = "penalty_committed")
    private Integer penaltyCommitted;
    @Column(name = "penalty_scored")
    private Integer penaltyScored;
    @Column(name = "penalty_missed")
    private Integer penaltyMissed;
    @Column(name = "penalty_saved")
    private Integer penaltySaved;
    @Column(name = "substitute")
    private Boolean substitute;
    @Column(name = "offsides")
    private Integer offsides;
    @Column(name = "passes_accurate")
    private Integer passesAccurate;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static FixturePlayerStatistics of(Fixture fixture, Team team, Player player) {
        return new FixturePlayerStatistics(fixture, team, player);
    }
    private FixturePlayerStatistics(Fixture fixture, Team team, Player player) {
        this.fixture = fixture;
        this.team = team;
        this.player = player;
    }
    public void update(
            Integer minutes,
            Integer number,
            String position,
            BigDecimal rating,
            Boolean captain,
            Integer shotsTotal,
            Integer shotsOn,
            Integer goals,
            Integer goalsConceded,
            Integer assists,
            Integer saves,
            Integer passesTotal,
            Integer passesKey,
            Integer tacklesTotal,
            Integer blocks,
            Integer interceptions,
            Integer duelsTotal,
            Integer duelsWon,
            Integer dribblesAttempts,
            Integer dribblesSuccess,
            Integer dribbledPast,
            Integer foulsDrawn,
            Integer foulsCommitted,
            Integer yellowCards,
            Integer redCards,
            Integer penaltyWon,
            Integer penaltyCommitted,
            Integer penaltyScored,
            Integer penaltyMissed,
            Integer penaltySaved,
            Boolean substitute,
            Integer offsides,
            Integer passesAccurate
    ) {
        this.minutes = minutes;
        this.number = number;
        this.position = position;
        this.rating = rating;
        this.captain = captain;
        this.shotsTotal = shotsTotal;
        this.shotsOn = shotsOn;
        this.goals = goals;
        this.goalsConceded = goalsConceded;
        this.assists = assists;
        this.saves = saves;
        this.passesTotal = passesTotal;
        this.passesKey = passesKey;
        this.tacklesTotal = tacklesTotal;
        this.blocks = blocks;
        this.interceptions = interceptions;
        this.duelsTotal = duelsTotal;
        this.duelsWon = duelsWon;
        this.dribblesAttempts = dribblesAttempts;
        this.dribblesSuccess = dribblesSuccess;
        this.dribbledPast = dribbledPast;
        this.foulsDrawn = foulsDrawn;
        this.foulsCommitted = foulsCommitted;
        this.yellowCards = yellowCards;
        this.redCards = redCards;
        this.penaltyWon = penaltyWon;
        this.penaltyCommitted = penaltyCommitted;
        this.penaltyScored = penaltyScored;
        this.penaltyMissed = penaltyMissed;
        this.penaltySaved = penaltySaved;
        this.substitute = substitute;
        this.offsides = offsides;
        this.passesAccurate = passesAccurate;
        this.updatedAt = Instant.now();
    }
}
