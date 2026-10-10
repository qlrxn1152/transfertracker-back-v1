package com.dhoon.transfertracker.internal.admin.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminDbSummaryItemResponseDto {

    private String tableName;
    private long count;
    private String description;

    public static AdminDbSummaryItemResponseDto of(String tableName, long count, String description) {
        return new AdminDbSummaryItemResponseDto(tableName, count, description);
    }
}
