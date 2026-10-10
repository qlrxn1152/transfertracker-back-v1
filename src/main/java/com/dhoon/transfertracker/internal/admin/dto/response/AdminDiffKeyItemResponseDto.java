package com.dhoon.transfertracker.internal.admin.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminDiffKeyItemResponseDto {

    private String entity;
    private String tableName;
    private Long primaryKey;
    private String naturalKey;
    private String label;

    public static AdminDiffKeyItemResponseDto of(String entity, String tableName, Long primaryKey, String naturalKey, String label) {
        return new AdminDiffKeyItemResponseDto(entity, tableName, primaryKey, naturalKey, label);
    }
}
