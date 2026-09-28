package com.dhoon.transfertracker.internal.member.dto.response;

import com.dhoon.transfertracker.internal.member.domain.Member;
import com.dhoon.transfertracker.internal.member.dto.request.MemberSignUpRequestDto;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberSignUpResponseDto {

    private Long memberId;
    private String loginId;

    public static MemberSignUpResponseDto of(Member member) {
        return new MemberSignUpResponseDto(member.getId(), member.getLoginId());
    }
}
