package com.dhoon.transfertracker.internal.repository;

import com.dhoon.transfertracker.internal.domain.TransferPost;
import com.dhoon.transfertracker.internal.domain.TransferPostSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransferPostRepository extends JpaRepository<TransferPost, Long> {

    Optional<TransferPost> findByExternalPostId(String externalPostId);

    List<TransferPost> findAllBySourcer(TransferPostSource sourcer);
}
