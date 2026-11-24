package com.dhoon.transfertracker.internal.post.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table
public class PostSyncStatus {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_sync_status_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false, unique = true)
    private Post post;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_step", nullable = false)
    private PostSyncStep currentStep;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_step_status", nullable = false)
    private PostSyncStatusType currentStepStatus;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static PostSyncStatus createPostSyncStatus(Post post) {
        return new PostSyncStatus(post);
    }

    private PostSyncStatus(Post post) {
        this.post = post;

        this.currentStep = PostSyncStep.ASSIGN;
        this.currentStepStatus = PostSyncStatusType.PENDING;
        this.retryCount = 0;
        this.errorMessage = null;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void start() {
        if ( currentStepStatus != PostSyncStatusType.PENDING ) {
            throw new IllegalStateException("PENDING 상태가 아닙니다.");
        }

        currentStepStatus = PostSyncStatusType.PROCESSING;
        this.updatedAt = LocalDateTime.now();
    }

    public void complete() {
        if ( currentStepStatus != PostSyncStatusType.PROCESSING ) {
            throw new IllegalStateException("PROCESSING 상태가 아닙니다.");
        }

        PostSyncStep nextStep = currentStep.next();

        if (nextStep == null) {
            currentStepStatus = PostSyncStatusType.COMPLETED;
        }
        else {
            currentStep = nextStep;
            currentStepStatus = PostSyncStatusType.PENDING;
        }

        this.retryCount = 0;
        this.errorMessage = null;
        this.updatedAt = LocalDateTime.now();
    }

    public void fail(String errorMessage) {
        if (currentStepStatus != PostSyncStatusType.PROCESSING) {
            throw new IllegalStateException("PROCESSING 상태가 아닙니다.");
        }

        currentStepStatus = PostSyncStatusType.FAILED;
        this.updatedAt = LocalDateTime.now();
        this.errorMessage = errorMessage;
    }

    public void retry() {
        if (currentStepStatus != PostSyncStatusType.FAILED) {
            throw new IllegalStateException("FAILED 상태가 아닙니다.");
        }

        currentStepStatus = PostSyncStatusType.PROCESSING;
        this.retryCount += 1;
        this.updatedAt = LocalDateTime.now();
    }
}
