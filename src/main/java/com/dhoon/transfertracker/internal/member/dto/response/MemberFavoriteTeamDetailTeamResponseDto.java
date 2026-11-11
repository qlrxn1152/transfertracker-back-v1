package com.dhoon.transfertracker.internal.member.dto.response;

import com.dhoon.transfertracker.internal.team.domain.Team;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberFavoriteTeamDetailTeamResponseDto {

    private Long teamId;
    private String teamName;
    private String logoUrl;

    public static MemberFavoriteTeamDetailTeamResponseDto of(Team team) {
        return new MemberFavoriteTeamDetailTeamResponseDto(
                team.getId(),
                team.getDisplayName(),
                "https://media.api-sports.io/football/teams/" + team.getApiFootballId() + ".png"
        );
    }
}
