package com.dhoon.transfertracker.internal.member.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberFavoriteTeamCreateResponseDto {

    private Long memberId;
    private Map<Long, String> teams;

    public static  MemberFavoriteTeamCreateResponseDto of(Long memberId, Map<Long, String> teams) {
        return new MemberFavoriteTeamCreateResponseDto(memberId, teams);
    }

}
