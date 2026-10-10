package com.dhoon.transfertracker.internal.admin.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminDryRunResponseDto {

    private List<AdminDryRunJobResponseDto> jobs;

    public static AdminDryRunResponseDto of(List<AdminDryRunJobResponseDto> jobs) {
        return new AdminDryRunResponseDto(jobs);
    }
}
