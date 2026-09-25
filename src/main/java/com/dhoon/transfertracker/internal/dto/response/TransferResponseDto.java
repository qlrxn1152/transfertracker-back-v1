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

    private Long playerId;
    private String playerName;
    private String photoUrl;
    private String inTeamName;
    private String outTeamName;
    private LocalDate date;
    private String type;

    public static TransferResponseDto of(Transfer transfer) {
        return new TransferResponseDto(
                transfer.getPlayer().getId(),
                transfer.getPlayer().getPlayerName(),
                "https://media.api-sports.io/football/players/" + transfer.getPlayer().getApiFootballId() + ".png",
                transfer.getInTeam().getTeamName(),
                transfer.getOutTeam().getTeamName(),
                transfer.getTransferDate(),
                transfer.getTransferType()
        );
    }
}
