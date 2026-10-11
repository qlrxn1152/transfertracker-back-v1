package com.dhoon.transfertracker.internal.transferpost.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TransferPostSourcesResponseDto {

    private List<TransferPostSourceItemResponseDto> sources = new ArrayList<>();

    public static TransferPostSourcesResponseDto of(List<TransferPostSourceItemResponseDto> sources) {
        return new TransferPostSourcesResponseDto(sources);
    }
}
