package com.dhoon.transfertracker.internal.member.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberFollowJournalistsResponseDto {

    List<MemberFollowJournalistDetailResponseDto> memberFollowJournalists;

    public static MemberFollowJournalistsResponseDto of(List<MemberFollowJournalistDetailResponseDto> memberFollowJournalists) {
        return new MemberFollowJournalistsResponseDto(memberFollowJournalists);
    }


}
