package com.dhoon.transfertracker.internal.post.dto.response;

import com.dhoon.transfertracker.internal.post.domain.Post;
import com.dhoon.transfertracker.internal.post.domain.PostJournalist;
import com.dhoon.transfertracker.internal.post.domain.PostTeam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PostItemResponseDto {

    private Long postId;
    private String externalPostId;
    private String content;
    private PostJournalist source;
    private Instant postCreatedAt;
    private boolean isRelateTransfer;
    private Long teamId;
    private String teamName;

    public static PostItemResponseDto of(Post post) {
        return new PostItemResponseDto(
                post.getId(),
                post.getExternalPostId(),
                post.getContent(),
                post.getJournalist(),
                post.getContentCreatedAt(),
                post.isTransferRelated(),
                null,
                null
        );
    }

    public static PostItemResponseDto of(PostTeam postTeam) {
        Post post = postTeam.getPost();
        return new PostItemResponseDto(
                post.getId(),
                post.getExternalPostId(),
                post.getContent(),
                post.getJournalist(),
                post.getContentCreatedAt(),
                post.isTransferRelated(),
                postTeam.getTeam().getId(),
                postTeam.getTeam().getDisplayName()
        );
    }
}
