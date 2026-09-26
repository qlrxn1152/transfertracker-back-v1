package com.dhoon.transfertracker.internal.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_team_player",
                        columnNames = {"team_id", "player_id"}
                )
        }
)
public class TeamPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_player_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false, unique = true)
    private Player player; // 현재 소속팀만 나타냄.

    private TeamPlayer(Team team, Player player) {
        this.team = team;
        this.player = player;
    }

    public static TeamPlayer of(Team team, Player player) {
        return new TeamPlayer(team, player);
    }
}
