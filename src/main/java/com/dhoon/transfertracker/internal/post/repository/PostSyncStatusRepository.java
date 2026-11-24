package com.dhoon.transfertracker.internal.post.repository;

import com.dhoon.transfertracker.internal.post.domain.PostSyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostSyncStatusRepository extends JpaRepository<PostSyncStatus, Long> {

    Optional<PostSyncStatus> findByPostId(Long postId);
}
