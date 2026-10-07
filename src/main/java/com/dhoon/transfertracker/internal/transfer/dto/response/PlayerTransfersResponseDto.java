package com.dhoon.transfertracker.internal.transfer.dto.response;

import com.dhoon.transfertracker.internal.player.domain.Player;
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

    public static PlayerTransfersResponseDto of(Player player, List<PlayerTransferItemResponseDto> playerTransfers) {
        return new PlayerTransfersResponseDto(player.getDisplayName(), playerTransfers);
    }

}
