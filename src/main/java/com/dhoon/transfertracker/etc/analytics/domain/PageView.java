package com.dhoon.transfertracker.etc.analytics.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PageView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "page_view_id")
    private Long id;

    @Column(nullable = false, length = 80)
    private String visitorId;

    @Column(nullable = false, length = 80)
    private String sessionId;

    @Column(nullable = false, length = 500)
    private String path;

    @Column(length = 200)
    private String pageTitle;

    @Column(length = 500)
    private String referrer;

    @Column(length = 500)
    private String userAgent;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private PageView(String visitorId, String sessionId, String path, String pageTitle, String referrer, String userAgent) {
        this.visitorId = visitorId;
        this.sessionId = sessionId;
        this.path = path;
        this.pageTitle = pageTitle;
        this.referrer = referrer;
        this.userAgent = userAgent;
    }

    public static PageView of(String visitorId, String sessionId, String path, String pageTitle, String referrer, String userAgent) {
        return new PageView(visitorId, sessionId, path, pageTitle, referrer, userAgent);
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
