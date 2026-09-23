package com.dhoon.transfertracker.internal.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerTransfersResponseDto {

    private String playerName;
    private List<PlayerTransferItemResponseDto> playerTransfers = new ArrayList<>();

    public static PlayerTransfersResponseDto of(String playerName, List<PlayerTransferItemResponseDto> playerTransfers) {
        return new PlayerTransfersResponseDto(playerName, playerTransfers);
    }

}
