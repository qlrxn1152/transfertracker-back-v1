package com.dhoon.transfertracker.internal.admin.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminJobLogsResponseDto {

    private List<AdminJobLogResponseDto> logs;

    public static AdminJobLogsResponseDto of(List<AdminJobLogResponseDto> logs) {
        return new AdminJobLogsResponseDto(logs);
    }
}
