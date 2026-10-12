package com.dhoon.transfertracker.etc.guide.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class GuidePostsResponseDto {

    private List<GuidePostItemResponseDto> guidePosts;

    public static GuidePostsResponseDto of(List<GuidePostItemResponseDto> guidePosts) {
        return new GuidePostsResponseDto(guidePosts);
    }
}
