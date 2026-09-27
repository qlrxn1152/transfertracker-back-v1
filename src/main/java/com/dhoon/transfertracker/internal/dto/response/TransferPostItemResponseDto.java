package com.dhoon.transfertracker.internal.dto.response;

import com.dhoon.transfertracker.internal.domain.TransferPost;
import com.dhoon.transfertracker.internal.domain.TransferPostSource;
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

    public static TransferPostItemResponseDto of(TransferPost transferPost) {
        return new TransferPostItemResponseDto(
                transferPost.getId(),
                transferPost.getExternalPostId(),
                transferPost.getContent(),
                transferPost.getSourcer(),
                transferPost.getContentCreatedAt()
        );
    }
}
