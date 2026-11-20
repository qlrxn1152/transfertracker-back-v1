package com.dhoon.transfertracker.internal.member.dto.response;

import com.dhoon.transfertracker.internal.member.domain.MemberFavoriteSourcer;
import com.dhoon.transfertracker.internal.post.domain.PostJournalist;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberFollowJournalistDetailResponseDto {

    private Long memberId;
    private PostJournalist journalist;
    private String displayName;

    public static MemberFollowJournalistDetailResponseDto of(MemberFavoriteSourcer memberFavoriteSourcer) {
        return new MemberFollowJournalistDetailResponseDto(
                memberFavoriteSourcer.getMember().getId(),
                memberFavoriteSourcer.getJournalist(),
                memberFavoriteSourcer.getJournalist().getDisplayerName()
        );
    }


}
