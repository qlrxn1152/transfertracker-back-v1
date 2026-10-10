package com.dhoon.transfertracker.internal.admin.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminOverviewResponseDto {

    private List<AdminMetricResponseDto> metrics;
    private List<AdminTaskResponseDto> priorityTasks;

    public static AdminOverviewResponseDto of(List<AdminMetricResponseDto> metrics, List<AdminTaskResponseDto> priorityTasks) {
        return new AdminOverviewResponseDto(metrics, priorityTasks);
    }
}
