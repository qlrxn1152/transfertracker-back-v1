package com.dhoon.transfertracker.internal.repository;

import com.dhoon.transfertracker.internal.domain.TransferPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransferPostRepository extends JpaRepository<TransferPost, Long> {

    Optional<TransferPost> findByExternalPostId(String externalPostId);
}
