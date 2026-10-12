package com.dhoon.transfertracker.etc.guide.dto.response;

import com.dhoon.transfertracker.etc.guide.domain.GuidePost;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class GuidePostItemResponseDto {

    private Long guidePostId;
    private String title;
    private String content;
    private List<String> imageUrls;
    private String authorLoginId;
    private LocalDateTime createdAt;

    public static GuidePostItemResponseDto of(GuidePost guidePost) {
        return new GuidePostItemResponseDto(
                guidePost.getId(),
                guidePost.getTitle(),
                guidePost.getContent(),
                guidePost.getImageUrls(),
                guidePost.getAuthorLoginId(),
                guidePost.getCreatedAt()
        );
    }
}
