package com.dhoon.transfertracker.internal.team.sync.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record TeamCollectionImportRequest(
        @NotEmpty List<@Valid TeamCollectionImportItem> teams
) {
}
