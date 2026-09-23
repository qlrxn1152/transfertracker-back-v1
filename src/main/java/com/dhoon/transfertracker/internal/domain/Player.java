package com.dhoon.transfertracker.internal.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Player {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "player_id")
    private Long id;

    @Column(name = "player_name", nullable = false)
    private String playerName;

    @Column(name = "api_football_id", nullable = false)
    private Long apiFootballId;

    private Player(String playerName, Long apiFootballId) {
        this.playerName = playerName;
        this.apiFootballId = apiFootballId;
    }

    public static  Player of(String playerName, Long apiFootballId) {
        return new Player(playerName, apiFootballId);
    }
}
