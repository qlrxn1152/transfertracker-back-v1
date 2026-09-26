package com.dhoon.transfertracker.internal.repository;

import com.dhoon.transfertracker.internal.domain.TransferPost;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferPostRepository extends JpaRepository<TransferPost, Long> {
}
