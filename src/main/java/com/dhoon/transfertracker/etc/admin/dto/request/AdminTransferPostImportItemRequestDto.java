package com.dhoon.transfertracker.etc.admin.dto.request;

import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@NoArgsConstructor
public class AdminTransferPostImportItemRequestDto {

    @NotBlank
    @Size(max = 100)
    private String externalPostId;

    @NotBlank
    @Size(max = 500)
    private String content;

    private TransferPostSource source;

    @NotNull
    private Instant postCreatedAt;

    private boolean isRelateTransfer;

    private String translatedContent;

    @Size(max = 1000)
    private String photoUrl;

    private String teamName;
}
