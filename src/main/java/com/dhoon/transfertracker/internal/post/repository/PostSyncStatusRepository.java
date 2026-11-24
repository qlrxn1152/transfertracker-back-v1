package com.dhoon.transfertracker.internal.post.repository;

import com.dhoon.transfertracker.internal.post.domain.PostSyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostSyncStatusRepository extends JpaRepository<PostSyncStatus, Long> {
}
