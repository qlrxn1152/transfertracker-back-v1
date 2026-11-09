package com.dhoon.transfertracker.internal.transferpost.dto.response;

import com.dhoon.transfertracker.internal.transferpost.domain.TransferPost;
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
public class TransferPostItemResponseDto {

    private Long postId;
    private String externalPostId;
    private String content;
    private TransferPostSource source;
    private Instant postCreatedAt;
    private boolean isRelateTransfer;
    private String translatedContent;
    private String photoUrl;

    private Long teamId;
    private String teamName;


    public static TransferPostItemResponseDto of(TransferPostTeam transferPostTeam) {
        return new TransferPostItemResponseDto(
                transferPostTeam.getPost().getId(),
                transferPostTeam.getPost().getExternalPostId(),
                transferPostTeam.getPost().getContent(),
                transferPostTeam.getPost().getSourcer(),
                transferPostTeam.getPost().getContentCreatedAt(),
                transferPostTeam.getPost().isTransferRelated(),
                transferPostTeam.getPost().getTranslatedContent() == null ? null : transferPostTeam.getPost().getTranslatedContent(),
                transferPostTeam.getPost().getPhotoUrl() == null ? null : transferPostTeam.getPost().getPhotoUrl(),

                transferPostTeam.getTeam().getId(),
                transferPostTeam.getTeam().getTeamName()
        );
    }

    public static TransferPostItemResponseDto of(TransferPost transferPost) {
        return new TransferPostItemResponseDto(
                transferPost.getId(),
                transferPost.getExternalPostId(),
                transferPost.getContent(),
                transferPost.getSourcer(),
                transferPost.getContentCreatedAt(),
                transferPost.isTransferRelated(),
                transferPost.getTranslatedContent() == null ? null : transferPost.getTranslatedContent(),
                transferPost.getPhotoUrl() == null ? null : transferPost.getPhotoUrl(),
                null,
                null


        );
    }


}
