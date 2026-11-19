package com.dhoon.transfertracker.internal.post.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PostsResponseDto {

    private List<PostItemResponseDto> posts;
    private long totalPostCount;
    private int totalPageCount;
    private boolean hasNext;
    private boolean hasPrevious;

    public static PostsResponseDto of(List<PostItemResponseDto> posts, long totalPostCount, int totalPageCount, boolean hasNext, boolean hasPrevious) {
        return new PostsResponseDto(posts, totalPostCount, totalPageCount, hasNext, hasPrevious);
    }
}
