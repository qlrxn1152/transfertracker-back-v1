package com.dhoon.transfertracker.internal.member.dto.response;

import com.dhoon.transfertracker.internal.transferpost.dto.response.TransferPostsPageResponseDto;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberFavoriteSourcersAllPostsResponseDto {

    List<TransferPostsPageResponseDto> allJournalistsPosts;

    public static MemberFavoriteSourcersAllPostsResponseDto of(List<TransferPostsPageResponseDto> allJournalistsPosts) {
        return new MemberFavoriteSourcersAllPostsResponseDto(allJournalistsPosts);
    }
}
