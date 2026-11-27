package com.dhoon.transfertracker.internal.standing.dto.record;

import java.util.List;

public record LeagueStandingGroupResponse(
        String groupName,
        List<LeagueStandingResponse> standings
) {
}