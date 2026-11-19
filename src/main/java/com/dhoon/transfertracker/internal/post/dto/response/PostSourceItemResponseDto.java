package com.dhoon.transfertracker.internal.post.dto.response;

import com.dhoon.transfertracker.internal.post.domain.PostJournalist;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PostSourceItemResponseDto {

    private PostJournalist source;
    private String xUsername;
    private String displayName;

    public static PostSourceItemResponseDto of(PostJournalist source) {
        return new PostSourceItemResponseDto(source, source.getXUsername(), source.getDisplayerName());
    }
}
