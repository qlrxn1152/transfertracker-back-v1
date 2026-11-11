package com.dhoon.transfertracker.internal.member.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberFavoriteTeamDetailResponseDto {

    private MemberFavoriteTeamDetailMemberResponseDto memberDetail;

    private List<MemberFavoriteTeamDetailTeamResponseDto> teamDetails;

    private List<MemberFavoriteTeamDetailPostResponseDto> postDetails;


    public static MemberFavoriteTeamDetailResponseDto of(
            MemberFavoriteTeamDetailMemberResponseDto memberDetail,
            List<MemberFavoriteTeamDetailTeamResponseDto> teamDetails,
            List<MemberFavoriteTeamDetailPostResponseDto> postDetails) {
        return new MemberFavoriteTeamDetailResponseDto(
                memberDetail,
                teamDetails,
                postDetails
        );
    }



}
