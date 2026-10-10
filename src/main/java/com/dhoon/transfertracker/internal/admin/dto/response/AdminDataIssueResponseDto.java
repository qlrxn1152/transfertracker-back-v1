package com.dhoon.transfertracker.internal.admin.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminDataIssueResponseDto {

    private String area;
    private String target;
    private String issue;
    private String action;
    private String tone;

    public static AdminDataIssueResponseDto of(String area, String target, String issue, String action, String tone) {
        return new AdminDataIssueResponseDto(area, target, issue, action, tone);
    }
}
