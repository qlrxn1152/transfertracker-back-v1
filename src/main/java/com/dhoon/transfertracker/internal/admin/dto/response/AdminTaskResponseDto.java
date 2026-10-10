package com.dhoon.transfertracker.internal.admin.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminTaskResponseDto {

    private String title;
    private String detail;
    private String status;
    private String tone;

    public static AdminTaskResponseDto of(String title, String detail, String status, String tone) {
        return new AdminTaskResponseDto(title, detail, status, tone);
    }
}
