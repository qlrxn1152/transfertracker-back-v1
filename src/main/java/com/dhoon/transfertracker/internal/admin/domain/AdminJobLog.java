package com.dhoon.transfertracker.internal.admin.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminJobLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "admin_job_log_id")
    private Long id;

    @Column(nullable = false)
    private String job;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(nullable = false)
    private long affectedCount;

    @Column(nullable = false, length = 1000)
    private String detail;

    @Column(nullable = false)
    private String environment;

    @Column(nullable = false)
    private Instant createdAt;

    private AdminJobLog(String job, String status, String message, long affectedCount, String detail, String environment) {
        this.job = job;
        this.status = status;
        this.message = message;
        this.affectedCount = affectedCount;
        this.detail = detail;
        this.environment = environment;
        this.createdAt = Instant.now();
    }

    public static AdminJobLog of(String job, String status, String message, long affectedCount, String detail, String environment) {
        return new AdminJobLog(job, status, message, affectedCount, detail, environment);
    }
}
