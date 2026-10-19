package com.dhoon.transfertracker.internal.transfer.query.projection;

import java.time.LocalDate;

public interface TransferSearchProjection {

    Long getTransferId();

    Long getPlayerId();
    String getPlayerName();
    Long getPlayerApiFootballId();

    Long getInTeamId();
    String getInTeamName();

    Long getOutTeamId();
    String getOutTeamName();

    LocalDate getTransferDate();
    String getTransferType();
}