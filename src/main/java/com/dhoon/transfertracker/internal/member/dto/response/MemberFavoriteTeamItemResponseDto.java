package com.dhoon.transfertracker.internal.member.dto.response;


import com.dhoon.transfertracker.internal.member.domain.MemberFavoriteTeam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberFavoriteTeamItemResponseDto {

    private Long favoriteId;
    private Long teamId;
    private String teamName;
    private String logoUrl;

    public static MemberFavoriteTeamItemResponseDto of(MemberFavoriteTeam memberFavoriteTeam) {
        return new MemberFavoriteTeamItemResponseDto(
                memberFavoriteTeam.getId(),
                memberFavoriteTeam.getTeam().getId(),
                memberFavoriteTeam.getTeam().getDisplayName(),
                "https://media.api-sports.io/football/teams/" + memberFavoriteTeam.getTeam().getApiFootballId() + ".png"
        );
    }
}
