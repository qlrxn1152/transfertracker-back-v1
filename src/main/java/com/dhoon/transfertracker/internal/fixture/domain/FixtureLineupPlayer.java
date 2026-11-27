package com.dhoon.transfertracker.internal.fixture.domain;

import com.dhoon.transfertracker.internal.player.domain.Player;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "fixture_lineup_player",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_fixture_lineup_player_lineup_player", columnNames = {"fixture_lineup_id", "player_id"})
        }
)
public class FixtureLineupPlayer {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fixture_lineup_player_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fixture_lineup_id", nullable = false)
    private FixtureLineup fixtureLineup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(name = "number")
    private Integer number;

    @Column(name = "position")
    private String position;

    @Column(name = "grid")
    private String grid;

    @Enumerated(EnumType.STRING)
    @Column(name = "fixture_lineup_player_status", nullable = false)
    private FixtureLineupPlayerStatus status;

    public static FixtureLineupPlayer createFixtureLineupPlayer(FixtureLineup fixtureLineup, Player player, Integer number, String position, String grid, FixtureLineupPlayerStatus status) {
        return new FixtureLineupPlayer(
                fixtureLineup,
                player,
                number,
                position,
                grid,
                status
        );
    }

    private FixtureLineupPlayer(FixtureLineup fixtureLineup, Player player, Integer number, String position, String grid, FixtureLineupPlayerStatus status) {
        this.fixtureLineup = fixtureLineup;
        this.player = player;
        this.number = number;
        this.position = position;
        this.grid = grid;
        this.status = status;
    }
}
