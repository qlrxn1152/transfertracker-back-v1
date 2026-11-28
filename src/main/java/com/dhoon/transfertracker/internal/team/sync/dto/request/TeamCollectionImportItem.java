package com.dhoon.transfertracker.internal.team.sync.dto.request;

import com.dhoon.transfertracker.internal.team.domain.TeamCollectionLeague;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TeamCollectionImportItem(
        @NotNull Long apiFootballId,
        @NotBlank String teamName,
        String country,
        Boolean national,
        @NotNull TeamCollectionLeague collectionLeagueCode
) {
}
