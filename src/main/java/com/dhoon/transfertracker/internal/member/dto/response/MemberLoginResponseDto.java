package com.dhoon.transfertracker.internal.member.dto.response;

import com.dhoon.transfertracker.internal.member.domain.Member;
import com.dhoon.transfertracker.internal.member.domain.MemberGrade;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberLoginResponseDto {

    private Long memberId;
    private String loginId;
    private MemberGrade memberGrade;
    private String accessToken;

    public static MemberLoginResponseDto of(Member member, String accessToken) {
        return new MemberLoginResponseDto(member.getId(), member.getLoginId(), member.getMemberGrade(), accessToken);
    }

}
