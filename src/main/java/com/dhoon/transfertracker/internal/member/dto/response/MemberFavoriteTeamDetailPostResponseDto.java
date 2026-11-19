package com.dhoon.transfertracker.internal.member.dto.response;

import com.dhoon.transfertracker.internal.post.domain.PostTeam;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberFavoriteTeamDetailPostResponseDto {

    private Long teamId;
    private String teamName;
    private String teamLogoUrl;

    private Long postId;
    private String postContent;
    private String postPhotoUrl;
    private Instant postCreatedAt;

    private TransferPostSource sourcer;

    public static MemberFavoriteTeamDetailPostResponseDto of(PostTeam postTeam) {
        return new MemberFavoriteTeamDetailPostResponseDto(
                postTeam.getTeam().getId(),
                postTeam.getTeam().getDisplayName(),
                "https://media.api-sports.io/football/teams/" + postTeam.getTeam().getApiFootballId() + ".png",
                postTeam.getPost().getId(),
                postTeam.getPost().getContent(),
                null,
                postTeam.getPost().getContentCreatedAt(),
                TransferPostSource.valueOf(postTeam.getPost().getJournalist().name())
        );
    }


}
