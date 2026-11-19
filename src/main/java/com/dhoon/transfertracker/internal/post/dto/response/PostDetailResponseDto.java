package com.dhoon.transfertracker.internal.post.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PostDetailResponseDto {

    private PostItemResponseDto post;

    public static PostDetailResponseDto of(PostItemResponseDto post) {
        return new PostDetailResponseDto(post);
    }
}
