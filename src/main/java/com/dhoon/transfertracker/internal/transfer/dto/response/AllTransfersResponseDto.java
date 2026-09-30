package com.dhoon.transfertracker.internal.transfer.dto.response;

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
    private boolean hasNext;
    private boolean hasPrevious;

    public static AllTransfersResponseDto of(List<TransferResponseDto> transfers, boolean hasNext, boolean hasPrevious) {
        return new AllTransfersResponseDto(transfers, hasNext, hasPrevious);
    }

}
