package com.dhoon.transfertracker.internal.team.query.dto.response.record;

import com.dhoon.transfertracker.internal.post.dto.response.PostsResponseDto;
import com.dhoon.transfertracker.internal.teamplayer.query.dto.response.TeamPlayersResponseDto;

public record TeamDetailResponseDto(
        TeamSummaryResponseDto teams,
        TeamPlayersResponseDto players,
        PostsResponseDto posts
) {

    public static TeamDetailResponseDto of(TeamSummaryResponseDto team, TeamPlayersResponseDto players, PostsResponseDto posts) {
        return new TeamDetailResponseDto(team, players, posts);
    }
}
