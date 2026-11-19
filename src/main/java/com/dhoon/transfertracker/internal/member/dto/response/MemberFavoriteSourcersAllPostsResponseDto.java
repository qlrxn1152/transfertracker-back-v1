package com.dhoon.transfertracker.internal.member.dto.response;

import com.dhoon.transfertracker.internal.post.dto.response.PostsResponseDto;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberFavoriteSourcersAllPostsResponseDto {

    List<PostsResponseDto> allJournalistsPosts;

    public static MemberFavoriteSourcersAllPostsResponseDto of(List<PostsResponseDto> allJournalistsPosts) {
        return new MemberFavoriteSourcersAllPostsResponseDto(allJournalistsPosts);
    }
}
