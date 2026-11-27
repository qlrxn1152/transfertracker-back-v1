package com.dhoon.transfertracker.internal.player.domain;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.Instant;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "player_season_statistics", uniqueConstraints = @UniqueConstraint(name = "uk_player_season_statistics_key", columnNames = {"player_id", "team_id", "league_code", "season"}))
public class PlayerSeasonStatistics {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "player_season_statistics_id")
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;
    @Enumerated(EnumType.STRING)
    @Column(name = "league_code", nullable = false)
    private LeagueCode leagueCode;
    @Column(name = "season", nullable = false)
    private Integer season;
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
    @Column(name = "appearances")
    private Integer appearances;
    @Column(name = "lineups")
    private Integer lineups;
    @Column(name = "substitutes_in")
    private Integer substitutesIn;
    @Column(name = "substitutes_out")
    private Integer substitutesOut;
    @Column(name = "substitutes_bench")
    private Integer substitutesBench;
    @Column(name = "yellow_red_cards")
    private Integer yellowRedCards;
    @Column(name = "passes_accuracy")
    private Integer passesAccuracy;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static PlayerSeasonStatistics of(Player player, Team team, LeagueCode leagueCode, Integer season) {
        return new PlayerSeasonStatistics(player, team, leagueCode, season);
    }
    private PlayerSeasonStatistics(Player player, Team team, LeagueCode leagueCode, Integer season) {
        this.player = player;
        this.team = team;
        this.leagueCode = leagueCode;
        this.season = season;
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
            Integer appearances,
            Integer lineups,
            Integer substitutesIn,
            Integer substitutesOut,
            Integer substitutesBench,
            Integer yellowRedCards,
            Integer passesAccuracy
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
        this.appearances = appearances;
        this.lineups = lineups;
        this.substitutesIn = substitutesIn;
        this.substitutesOut = substitutesOut;
        this.substitutesBench = substitutesBench;
        this.yellowRedCards = yellowRedCards;
        this.passesAccuracy = passesAccuracy;
        this.updatedAt = Instant.now();
    }
}
