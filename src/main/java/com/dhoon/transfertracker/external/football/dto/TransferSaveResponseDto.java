package com.dhoon.transfertracker.external.football.dto;

import com.dhoon.transfertracker.internal.domain.Player;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TransferSaveResponseDto {

    private String playerName;
    private Long playerAPIID;

    public static TransferSaveResponseDto of(Player player) {
        return new TransferSaveResponseDto(player.getPlayerName(), player.getApiFootballId());
    }
}
