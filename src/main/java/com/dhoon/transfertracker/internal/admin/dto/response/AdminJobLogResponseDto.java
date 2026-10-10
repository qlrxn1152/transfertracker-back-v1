package com.dhoon.transfertracker.internal.admin.dto.response;

import com.dhoon.transfertracker.internal.admin.domain.AdminJobLog;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminJobLogResponseDto {

    private Long id;
    private String job;
    private String status;
    private String message;
    private long affectedCount;
    private String detail;
    private String environment;
    private Instant createdAt;
    private String tone;

    public static AdminJobLogResponseDto of(AdminJobLog log) {
        return new AdminJobLogResponseDto(
                log.getId(),
                log.getJob(),
                log.getStatus(),
                log.getMessage(),
                log.getAffectedCount(),
                log.getDetail(),
                log.getEnvironment(),
                log.getCreatedAt(),
                resolveTone(log.getStatus())
        );
    }

    private static String resolveTone(String status) {
        if ("COMPLETED".equals(status)) {
            return "good";
        }

        if ("FAILED".equals(status)) {
            return "bad";
        }

        return "neutral";
    }
}
