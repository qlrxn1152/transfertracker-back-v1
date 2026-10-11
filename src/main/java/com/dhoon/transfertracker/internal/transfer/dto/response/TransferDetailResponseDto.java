package com.dhoon.transfertracker.internal.transfer.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TransferDetailResponseDto {

    private TransferResponseDto transfer;

    public static TransferDetailResponseDto of(TransferResponseDto transfer) {
        return new TransferDetailResponseDto(transfer);
    }
}
