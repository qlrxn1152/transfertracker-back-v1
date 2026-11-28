package com.dhoon.transfertracker.internal.team.query.dto.response.record;

import com.dhoon.transfertracker.internal.team.domain.Team;

public record TeamItemResponseDto(Long teamId, String teamName, String logoUrl) {

    public static TeamItemResponseDto of(Team team) {
        return new TeamItemResponseDto(
                team.getId(),
                team.getDisplayName(),
                "https://media.api-sports.io/football/teams/" + team.getApiFootballId() + ".png"
        );
    }

}
