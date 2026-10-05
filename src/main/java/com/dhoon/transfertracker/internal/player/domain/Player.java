package com.dhoon.transfertracker.internal.player.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        indexes = @Index(name = "idx_player_player_name", columnList = "playerName")
)
public class Player {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "player_id")
    private Long id;

    @Column(name = "player_name", nullable = false)
    private String playerName;

    @Column(name = "api_football_id", nullable = false, unique = true)
    private Long apiFootballId;

    @Column(name = "player_name_ko")
    private String playerNameKo;

    private Player(String playerName, Long apiFootballId) {
        this.playerName = playerName;
        this.apiFootballId = apiFootballId;
    }

    public static  Player of(String playerName, Long apiFootballId) {
        return new Player(playerName, apiFootballId);
    }

    public void assignKoPlayerName(String playerNameKo) {
        this.playerNameKo = playerNameKo;
    }

    public String getDisplayName() {
        if (playerNameKo == null || playerNameKo.isBlank()) {
            return playerName;
        }

        return playerNameKo;
    }

}
