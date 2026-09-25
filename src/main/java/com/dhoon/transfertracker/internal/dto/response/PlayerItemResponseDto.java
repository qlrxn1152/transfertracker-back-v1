package com.dhoon.transfertracker.internal.dto.response;

import com.dhoon.transfertracker.internal.domain.Player;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerItemResponseDto {

    private Long playerId;
    private String playerName;
    private String photoUrl;

    public static PlayerItemResponseDto of(Player player) {
        return new PlayerItemResponseDto(
                player.getId(),
                player.getPlayerName(),
                "https://media.api-sports.io/football/players/" + player.getApiFootballId() + ".png"
                );
    }
}
