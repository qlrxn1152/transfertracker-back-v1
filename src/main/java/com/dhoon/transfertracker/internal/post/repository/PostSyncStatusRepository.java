package com.dhoon.transfertracker.internal.post.repository;

import com.dhoon.transfertracker.internal.post.domain.PostSyncStatus;
import com.dhoon.transfertracker.internal.post.domain.PostSyncStatusType;
import com.dhoon.transfertracker.internal.post.domain.PostSyncStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PostSyncStatusRepository extends JpaRepository<PostSyncStatus, Long> {

    Optional<PostSyncStatus> findByPostId(Long postId);

    long countByCurrentStepAndCurrentStepStatus(PostSyncStep currentStep, PostSyncStatusType currentStepStatus);

    long countByCurrentStepAndCurrentStepStatusAndUpdatedAtBefore(
            PostSyncStep currentStep,
            PostSyncStatusType currentStepStatus,
            LocalDateTime updatedAt
    );
}
