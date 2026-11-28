package com.dhoon.transfertracker.internal.team.sync.dto.record;

import java.util.List;

public record FootballDataSyncResponse(
        int targetLeagueCount,
        int targetTeamCount,
        int successTeamCount,
        int successSquadCount,
        int successTransferTeamCount,
        int processedSquadPlayerCount,
        int successTransferPlayerCount,
        int failedTransferPlayerCount,
        List<Failure> failures
) {

    public record Failure(
            Long leagueApiId,
            Long teamApiId,
            String stage,
            String message
    ) {
    }
}