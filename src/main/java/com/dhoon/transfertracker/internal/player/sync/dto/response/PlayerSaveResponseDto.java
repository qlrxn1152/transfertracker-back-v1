package com.dhoon.transfertracker.internal.player.sync.dto.response;

import com.dhoon.transfertracker.internal.player.domain.Player;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PlayerSaveResponseDto {

    private String playerName;
    private Long playerAPIId;

    public static PlayerSaveResponseDto of(Player player) {
        return new PlayerSaveResponseDto(player.getDisplayName(), player.getApiFootballId());
    }
}