package com.dhoon.transfertracker.internal.admin.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminDataQualityResponseDto {

    private List<AdminDataIssueResponseDto> issues;

    public static AdminDataQualityResponseDto of(List<AdminDataIssueResponseDto> issues) {
        return new AdminDataQualityResponseDto(issues);
    }
}
