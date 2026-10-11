package com.dhoon.transfertracker.internal.analytics.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AdminAnalyticsResponseDto {

    private List<AdminAnalyticsMetricResponseDto> metrics;
    private List<AdminPopularPageResponseDto> popularPages;

    public static AdminAnalyticsResponseDto of(
            List<AdminAnalyticsMetricResponseDto> metrics,
            List<AdminPopularPageResponseDto> popularPages
    ) {
        return new AdminAnalyticsResponseDto(metrics, popularPages);
    }
}
