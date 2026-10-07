package com.dhoon.transfertracker.internal.team.dto.response;

import com.dhoon.transfertracker.internal.team.domain.Team;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamPageInfoForTeamResponseDto {

    private Long teamId;
    private String teamName;
    private long teamPlayerCount;
    private String logoUrl;

    public static TeamPageInfoForTeamResponseDto of(Team team, long teamPlayerCount) {
        return new TeamPageInfoForTeamResponseDto(
                team.getId(),
                team.getDisplayName(),
                teamPlayerCount,
                "https://media.api-sports.io/football/teams/" + team.getApiFootballId() + ".png"
        );
    }




}
