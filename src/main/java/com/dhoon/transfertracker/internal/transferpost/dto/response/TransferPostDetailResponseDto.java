package com.dhoon.transfertracker.internal.transferpost.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TransferPostDetailResponseDto {

    private TransferPostItemResponseDto post;

    public static TransferPostDetailResponseDto of(TransferPostItemResponseDto post) {
        return new TransferPostDetailResponseDto(post);
    }
}
