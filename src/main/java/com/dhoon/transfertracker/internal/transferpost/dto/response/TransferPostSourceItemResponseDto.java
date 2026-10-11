package com.dhoon.transfertracker.internal.transferpost.dto.response;

import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TransferPostSourceItemResponseDto {

    private TransferPostSource source;
    private String xUsername;

    public static TransferPostSourceItemResponseDto of(TransferPostSource source) {
        return new TransferPostSourceItemResponseDto(source, source.getXUsername());
    }
}
