package com.dhoon.transfertracker.internal.member.dto.response;

import com.dhoon.transfertracker.internal.member.domain.Member;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberFavoriteTeamDetailMemberResponseDto {

    private Long memberId;
    private String username;

    public static MemberFavoriteTeamDetailMemberResponseDto of(Member member) {
        return new MemberFavoriteTeamDetailMemberResponseDto(
                member.getId(),
                member.getLoginId()
        );
    }
}
