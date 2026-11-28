package com.dhoon.transfertracker.internal.team.query.dto.response.record;

import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.TeamCollectionLeague;

public record TeamItemResponseDto(
        Long teamId,
        String teamName,
        String logoUrl,
        LeagueCode leagueCode,
        TeamCollectionLeague collectionLeagueCode
) {

    public TeamItemResponseDto(Long teamId, String teamName, String logoUrl) {
        this(teamId, teamName, logoUrl, null, null);
    }

    public static TeamItemResponseDto of(Team team) {
        return new TeamItemResponseDto(
                team.getId(),
                team.getDisplayName(),
                "https://media.api-sports.io/football/teams/" + team.getApiFootballId() + ".png",
                team.getLeagueCode(),
                team.getCollectionLeagueCode()
        );
    }

}
