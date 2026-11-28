package com.dhoon.transfertracker.internal.team.query.dto.response.record;

import com.dhoon.transfertracker.internal.team.domain.Team;

public record TeamSummaryResponseDto(Long teamId, String teamName, String logoUrl) {

    public static TeamSummaryResponseDto of(Team team) {
        return new TeamSummaryResponseDto(
                team.getId(),
                team.getDisplayName(),
                "https://media.api-sports.io/football/teams/" + team.getApiFootballId() + ".png"
        );
    }
}
