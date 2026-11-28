package com.dhoon.transfertracker.internal.team.sync.dto.record;

import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import tools.jackson.databind.JsonNode;

public record TeamCollectionTarget(
        Long teamApiId,
        String teamName,
        boolean national,
        LeagueCode domesticLeague,
        JsonNode teamData
) {
}