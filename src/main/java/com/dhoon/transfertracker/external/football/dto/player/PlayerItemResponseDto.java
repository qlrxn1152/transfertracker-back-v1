package com.dhoon.transfertracker.external.football.dto.player;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PlayerItemResponseDto {

    private Long playerId;
    private String playerName;
    private int playerAge;
    private int playerNumber;
    private String playerPosition;
}
