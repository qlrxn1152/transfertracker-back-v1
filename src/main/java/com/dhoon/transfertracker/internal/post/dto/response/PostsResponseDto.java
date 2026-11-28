package com.dhoon.transfertracker.internal.post.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

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

    public static PostsResponseDto of(Page<PostItemResponseDto> postPage) {
        return new PostsResponseDto(postPage.getContent(), postPage.getTotalElements(), postPage.getTotalPages(), postPage.hasNext(), postPage.hasPrevious());
    }
}
