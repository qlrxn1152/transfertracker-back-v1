package com.dhoon.transfertracker.internal.team.query.dto.response;

import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.TeamCollectionLeague;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamItemResponseDto {

    private Long teamId;
    private String teamName;
    private String logoUrl;
    private LeagueCode leagueCode;
    private TeamCollectionLeague collectionLeagueCode;

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
