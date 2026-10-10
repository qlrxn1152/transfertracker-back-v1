package com.dhoon.transfertracker.internal.admin.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminMetricResponseDto {

    private String label;
    private long value;
    private String status;
    private String tone;
    private String description;

    public static AdminMetricResponseDto of(String label, long value, String status, String tone, String description) {
        return new AdminMetricResponseDto(label, value, status, tone, description);
    }
}
