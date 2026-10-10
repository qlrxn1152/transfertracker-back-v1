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
public class TransferResponseDto {

    private Long playerId;
    private String playerName;
    private String photoUrl;

    private Long inTeamId;
    private String inTeamName;

    private Long outTeamId;
    private String outTeamName;

    private LocalDate date;
    private String type;

    public static TransferResponseDto of(Transfer transfer) {
        return new TransferResponseDto(
                transfer.getPlayer().getId(),
                transfer.getPlayer().getDisplayName(),
                "https://media.api-sports.io/football/players/" + transfer.getPlayer().getApiFootballId() + ".png",

                transfer.getInTeam().getId(),
                transfer.getInTeam().getDisplayName(),

                transfer.getOutTeam().getId(),
                transfer.getOutTeam().getDisplayName(),

                transfer.getTransferDate(),
                transfer.getTransferType()
        );
    }



}
