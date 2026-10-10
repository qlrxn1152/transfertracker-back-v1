package com.dhoon.transfertracker.internal.member.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberFavoriteTeamsResponseDto {

    private List<MemberFavoriteTeamItemResponseDto> favoriteTeams;

    public static MemberFavoriteTeamsResponseDto of(List<MemberFavoriteTeamItemResponseDto> favoriteTeams) {
        return new MemberFavoriteTeamsResponseDto(favoriteTeams);
    }
}
