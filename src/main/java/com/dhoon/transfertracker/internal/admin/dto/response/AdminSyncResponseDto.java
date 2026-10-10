package com.dhoon.transfertracker.internal.admin.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminSyncResponseDto {

    private String job;
    private String status;
    private String message;

    public static AdminSyncResponseDto of(String job, String status, String message) {
        return new AdminSyncResponseDto(job, status, message);
    }
}
