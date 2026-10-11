package com.dhoon.transfertracker.internal.analytics.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AdminPopularPageResponseDto {

    private String path;
    private long views;

    public static AdminPopularPageResponseDto of(String path, long views) {
        return new AdminPopularPageResponseDto(path, views);
    }
}
