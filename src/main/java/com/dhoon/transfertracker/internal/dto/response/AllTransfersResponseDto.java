package com.dhoon.transfertracker.internal.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AllTransfersResponseDto {

    private List<TransferResponseDto> transfers = new ArrayList<>();

    public static AllTransfersResponseDto of(List<TransferResponseDto> transfers) {
        return new AllTransfersResponseDto(transfers);
    }
}
