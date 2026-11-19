package com.dhoon.transfertracker.internal.post.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PostSourcesResponseDto {

    private List<PostSourceItemResponseDto> sources;

    public static PostSourcesResponseDto of(List<PostSourceItemResponseDto> sources) {
        return new PostSourcesResponseDto(sources);
    }
}
