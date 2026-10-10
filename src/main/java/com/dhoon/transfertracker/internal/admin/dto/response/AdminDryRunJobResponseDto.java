package com.dhoon.transfertracker.internal.admin.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminDryRunJobResponseDto {

    private String job;
    private String title;
    private long affectedCount;
    private String message;
    private String tone;

    public static AdminDryRunJobResponseDto of(String job, String title, long affectedCount, String message, String tone) {
        return new AdminDryRunJobResponseDto(job, title, affectedCount, message, tone);
    }
}
