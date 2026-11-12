package com.dhoon.transfertracker.internal.member.dto.response;

import com.dhoon.transfertracker.internal.member.domain.MemberFavoriteSourcer;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberFollowJournalistDetailResponseDto {

    private Long memberId;
    private TransferPostSource journalist;

    public static MemberFollowJournalistDetailResponseDto of(MemberFavoriteSourcer memberFavoriteSourcer) {
        return new MemberFollowJournalistDetailResponseDto(
                memberFavoriteSourcer.getMember().getId(),
                memberFavoriteSourcer.getJournalist()
        );
    }


}
