package com.dhoon.transfertracker.internal.admin.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminSourceResponseDto {

    private String source;
    private String xUsername;
    private long posts;
    private String status;
    private String tone;

    public static AdminSourceResponseDto of(String source, String xUsername, long posts, String status, String tone) {
        return new AdminSourceResponseDto(source, xUsername, posts, status, tone);
    }
}
