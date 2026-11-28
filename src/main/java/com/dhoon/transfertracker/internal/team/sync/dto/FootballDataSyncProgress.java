package com.dhoon.transfertracker.internal.team.sync.dto;

import java.time.Instant;

public record FootballDataSyncProgress(

        String status,

        int totalTeamCount,
        int processedTeamCount,
        int failedTeamCount,

        int successTeamCount,
        int successSquadCount,
        int successTransferTeamCount,

        double progressPercent,

        Instant startedAt,
        Instant finishedAt

) {
}