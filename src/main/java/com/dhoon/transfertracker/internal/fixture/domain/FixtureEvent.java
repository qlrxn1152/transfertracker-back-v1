package com.dhoon.transfertracker.internal.fixture.domain;

import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.team.domain.Team;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "fixture_event",
        indexes = {
                @Index(name = "idx_fixture_event_timeline", columnList = "fixture_id, elapsed, extra, fixture_event_id")
        }

)
public class FixtureEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fixture_event_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fixture_id", nullable = false)
    private Fixture fixture;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_player_id")
    private Player relatedPlayer;

    @Column(nullable = false)
    private Integer elapsed;

    private Integer extra;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FixtureEventType type;

    @Column(nullable = false)
    private String detail;

    private String comments;

    public static FixtureEvent createFixtureEvent(Fixture fixture, Team team, Player player, Player relatedPlayer, Integer elapsed, Integer extra, FixtureEventType type, String detail, String comments) {
        return new FixtureEvent(
                fixture,
                team,
                player,
                relatedPlayer,
                elapsed,
                extra,
                type,
                detail,
                comments
        );
    }

    private FixtureEvent(Fixture fixture, Team team, Player player, Player relatedPlayer, Integer elapsed, Integer extra, FixtureEventType type, String detail, String comments) {
        this.fixture = fixture;
        this.team = team;
        this.player = player;
        this.relatedPlayer = relatedPlayer;
        this.elapsed = elapsed;
        this.extra = extra;
        this.type = type;
        this.detail = detail;
        this.comments = comments;
    }
}