package com.dhoon.transfertracker.internal.dto.response;

import com.dhoon.transfertracker.internal.domain.Transfer;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TransferResponseDto {

    private String playerName;
    private String inTeamName;
    private String outTeamName;
    private LocalDate date;
    private String type;

    public static TransferResponseDto of(Transfer transfer) {
        return new TransferResponseDto(
                transfer.getPlayer().getPlayerName(),
                transfer.getInTeam().getTeamName(),
                transfer.getOutTeam().getTeamName(),
                transfer.getTransferDate(),
                transfer.getTransferType()
        );
    }
}
