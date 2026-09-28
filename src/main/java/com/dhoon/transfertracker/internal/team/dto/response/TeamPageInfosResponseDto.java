package com.dhoon.transfertracker.internal.team.dto.response;

import com.dhoon.transfertracker.internal.teamplayer.dto.response.TeamPlayersResponseDto;
import com.dhoon.transfertracker.internal.transfer.dto.response.TransferPostsResponseDto;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TeamPageInfosResponseDto {

    private TeamPageInfoForTeamResponseDto teams;
    private TeamPlayersResponseDto players;
    private TransferPostsResponseDto posts;

    public static TeamPageInfosResponseDto of(TeamPageInfoForTeamResponseDto teams, TeamPlayersResponseDto players, TransferPostsResponseDto posts) {
        return new TeamPageInfosResponseDto(teams, players, posts);
    }
}
