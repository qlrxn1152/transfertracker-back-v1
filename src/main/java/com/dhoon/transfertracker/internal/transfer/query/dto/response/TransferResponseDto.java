package com.dhoon.transfertracker.internal.transfer.query.dto.response;

import com.dhoon.transfertracker.internal.transfer.domain.Transfer;
import com.dhoon.transfertracker.internal.transfer.query.projection.TransferSearchProjection;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TransferResponseDto {

    private Long transferId;

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
                transfer.getId(),

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

    public static TransferResponseDto of(TransferSearchProjection transfer) {
        return new TransferResponseDto(
                transfer.getTransferId(),

                transfer.getPlayerId(),
                transfer.getPlayerName(),
                "https://media.api-sports.io/football/players/"
                        + transfer.getPlayerApiFootballId()
                        + ".png",

                transfer.getInTeamId(),
                transfer.getInTeamName(),

                transfer.getOutTeamId(),
                transfer.getOutTeamName(),

                transfer.getTransferDate(),
                transfer.getTransferType()
        );
    }



}
