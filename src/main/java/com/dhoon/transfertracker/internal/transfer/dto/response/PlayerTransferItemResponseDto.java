package com.dhoon.transfertracker.internal.transfer.dto.response;

import com.dhoon.transfertracker.internal.transfer.domain.Transfer;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerTransferItemResponseDto {

    private String inTeamName;
    private String outTeamName;
    private LocalDate date;
    private String type;

    public static PlayerTransferItemResponseDto of(Transfer transfer) {
        return new PlayerTransferItemResponseDto(
                transfer.getInTeam().getTeamName(),
                transfer.getOutTeam().getTeamName(),
                transfer.getTransferDate(),
                transfer.getTransferType()
        );
    }
}
