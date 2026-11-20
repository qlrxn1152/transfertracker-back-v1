package com.dhoon.transfertracker.internal.post.dto.response;

import com.dhoon.transfertracker.internal.post.domain.Post;
import com.dhoon.transfertracker.internal.post.domain.PostJournalist;
import com.dhoon.transfertracker.internal.post.domain.PostPlayer;
import com.dhoon.transfertracker.internal.post.domain.PostTeam;
import com.dhoon.transfertracker.internal.teamplayer.domain.TeamPlayer;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PostItemResponseDto {

    private Long postId;
    private String externalPostId;
    private String content;
    private PostJournalist journalist;
    private LocalDateTime contentCreatedAt;
    private boolean isRelateTransfer;

    private Long teamId;
    private String teamName;

    private Long playerId;
    private String playerName;

    public static PostItemResponseDto of(Post post) {
        return new PostItemResponseDto(
                post.getId(),
                post.getExternalPostId(),
                post.getContent(),
                post.getJournalist(),
                post.getContentCreatedAt(),
                post.isTransferRelated(),
                null,
                null,
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
                postTeam.getTeam().getDisplayName(),
                null,
                null
        );
    }

    public static PostItemResponseDto createFaPlayer(PostPlayer postPlayer) {
        Post post = postPlayer.getPost();

        return new PostItemResponseDto(
                post.getId(),
                post.getExternalPostId(),
                post.getContent(),
                post.getJournalist(),
                post.getContentCreatedAt(),
                post.isTransferRelated(),
                null,
                null,
                postPlayer.getPlayer().getId(),
                postPlayer.getPlayer().getDisplayName()
        );

    }

    public static PostItemResponseDto createTeamPlayer(PostPlayer postPlayer, TeamPlayer teamPlayer) {
        Post post = postPlayer.getPost();

        return new PostItemResponseDto(
                post.getId(),
                post.getExternalPostId(),
                post.getContent(),
                post.getJournalist(),
                post.getContentCreatedAt(),
                post.isTransferRelated(),

                teamPlayer.getTeam().getId(),
                teamPlayer.getTeam().getDisplayName(),



                postPlayer.getPlayer().getId(),
                postPlayer.getPlayer().getDisplayName()
        );
    }
}
