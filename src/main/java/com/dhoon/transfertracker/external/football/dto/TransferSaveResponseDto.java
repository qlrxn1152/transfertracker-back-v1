package com.dhoon.transfertracker.external.football.dto;

import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.transfer.domain.Transfer;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TransferSaveResponseDto {

    private Long transferId;
    private String playerName;
    private String inTeamName;
    private String outTeamName;
    private LocalDate transferDate;
    private String transferType;


    public static TransferSaveResponseDto of(Transfer transfer) {
        return new TransferSaveResponseDto(
                transfer.getId(),
                transfer.getPlayer().getDisplayName(),
                transfer.getInTeam().getDisplayName(),
                transfer.getOutTeam().getDisplayName(),
                transfer.getTransferDate(),
                transfer.getTransferType()
        );
    }

}
