package com.dhoon.transfertracker.internal.fixture.domain;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.Instant;
import com.dhoon.transfertracker.internal.team.domain.Team;
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "fixture_statistics", uniqueConstraints = @UniqueConstraint(name = "uk_fixture_statistics_key", columnNames = {"fixture_id", "team_id"}))
public class FixtureStatistics {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fixture_statistics_id")
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fixture_id", nullable = false)
    private Fixture fixture;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;
    @Column(name = "passes_accurate")
    private Integer passesAccurate;
    @Column(name = "shots_inside_box")
    private Integer shotsInsideBox;
    @Column(name = "shots_outside_box")
    private Integer shotsOutsideBox;
    @Column(name = "blocked_shots")
    private Integer blockedShots;
    @Column(name = "free_kicks")
    private Integer freeKicks;
    @Column(name = "shots_on_goal")
    private Integer shotsOnGoal;
    @Column(name = "goalkeeper_saves")
    private Integer goalkeeperSaves;
    @Column(name = "shots_off_goal")
    private Integer shotsOffGoal;
    @Column(name = "total_passes")
    private Integer totalPasses;
    @Column(name = "total_shots")
    private Integer totalShots;
    @Column(name = "yellow_cards")
    private Integer yellowCards;
    @Column(name = "corner_kicks")
    private Integer cornerKicks;
    @Column(name = "offsides")
    private Integer offsides;
    @Column(name = "fouls")
    private Integer fouls;
    @Column(name = "red_cards")
    private Integer redCards;
    @Column(name = "ball_possession")
    private Integer ballPossession;
    @Column(name = "expected_goals", precision = 12, scale = 6)
    private BigDecimal expectedGoals;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static FixtureStatistics of(Fixture fixture, Team team) {
        return new FixtureStatistics(fixture, team);
    }
    private FixtureStatistics(Fixture fixture, Team team) {
        this.fixture = fixture;
        this.team = team;
    }
    public void update(
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
        this.passesAccurate = passesAccurate;
        this.shotsInsideBox = shotsInsideBox;
        this.shotsOutsideBox = shotsOutsideBox;
        this.blockedShots = blockedShots;
        this.freeKicks = freeKicks;
        this.shotsOnGoal = shotsOnGoal;
        this.goalkeeperSaves = goalkeeperSaves;
        this.shotsOffGoal = shotsOffGoal;
        this.totalPasses = totalPasses;
        this.totalShots = totalShots;
        this.yellowCards = yellowCards;
        this.cornerKicks = cornerKicks;
        this.offsides = offsides;
        this.fouls = fouls;
        this.redCards = redCards;
        this.ballPossession = ballPossession;
        this.expectedGoals = expectedGoals;
        this.updatedAt = Instant.now();
    }
}
