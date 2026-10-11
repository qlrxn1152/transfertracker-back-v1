package com.dhoon.transfertracker.internal.analytics.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AdminAnalyticsMetricResponseDto {

    private String label;
    private long value;
    private String description;
    private String tone;

    public static AdminAnalyticsMetricResponseDto of(String label, long value, String description, String tone) {
        return new AdminAnalyticsMetricResponseDto(label, value, description, tone);
    }
}
