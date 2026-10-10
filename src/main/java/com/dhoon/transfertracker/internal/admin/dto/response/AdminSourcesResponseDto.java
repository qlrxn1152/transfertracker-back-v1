package com.dhoon.transfertracker.internal.admin.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminSourcesResponseDto {

    private List<AdminSourceResponseDto> sources;

    public static AdminSourcesResponseDto of(List<AdminSourceResponseDto> sources) {
        return new AdminSourcesResponseDto(sources);
    }
}
