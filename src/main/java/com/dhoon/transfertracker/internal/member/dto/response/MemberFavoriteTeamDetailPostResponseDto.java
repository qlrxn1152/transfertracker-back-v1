package com.dhoon.transfertracker.internal.member.dto.response;

import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostTeam;
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

    public static MemberFavoriteTeamDetailPostResponseDto of(TransferPostTeam transferPostTeam) {
        return new MemberFavoriteTeamDetailPostResponseDto(
                transferPostTeam.getTeam().getId(),
                transferPostTeam.getTeam().getDisplayName(),
                "https://media.api-sports.io/football/teams/" + transferPostTeam.getTeam().getApiFootballId() + ".png",

                transferPostTeam.getPost().getId(),
                transferPostTeam.getPost().getDisplayContent(),
                transferPostTeam.getPost().getPhotoUrl(),
                transferPostTeam.getPost().getContentCreatedAt(),

                transferPostTeam.getPost().getSourcer()
        );
    }


}
