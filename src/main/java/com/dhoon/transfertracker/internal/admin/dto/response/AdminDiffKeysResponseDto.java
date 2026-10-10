package com.dhoon.transfertracker.internal.admin.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminDiffKeysResponseDto {

    private List<AdminDiffKeyItemResponseDto> items;

    public static AdminDiffKeysResponseDto of(List<AdminDiffKeyItemResponseDto> items) {
        return new AdminDiffKeysResponseDto(items);
    }
}
