package com.dhoon.transfertracker.internal.fixture.domain;

import com.dhoon.transfertracker.internal.team.domain.Team;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "fixture_lineup",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_fixture_lineup_fixture_team", columnNames = {"fixture_id", "team_id"})
        }
)
public class FixtureLineup {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fixture_lineup_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fixture_id", nullable = false)
    private Fixture fixture;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(name = "formation")
    private String formation;

    public void updateFormation(String formation) {
        this.formation = formation;
    }

    public static FixtureLineup createFixtureLineup(Fixture fixture, Team team, String formation) {
        return new FixtureLineup(fixture, team, formation);
    }

    private FixtureLineup(Fixture fixture, Team team, String formation) {
        this.fixture = fixture;
        this.team = team;
        this.formation = formation;
    }
}
