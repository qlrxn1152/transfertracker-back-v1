package com.dhoon.transfertracker.internal.admin.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminDbSummaryResponseDto {

    private List<AdminDbSummaryItemResponseDto> tables;

    public static AdminDbSummaryResponseDto of(List<AdminDbSummaryItemResponseDto> tables) {
        return new AdminDbSummaryResponseDto(tables);
    }
}
