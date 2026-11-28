package com.dhoon.transfertracker.internal.team.sync.dto.response;

public record TeamCollectionImportResponse(
        int receivedCount,
        int createdCount,
        int updatedCount,
        int skippedNationalTeamCount
) {
}
