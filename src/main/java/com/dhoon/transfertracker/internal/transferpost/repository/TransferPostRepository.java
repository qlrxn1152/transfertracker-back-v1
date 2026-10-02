package com.dhoon.transfertracker.internal.transferpost.repository;

import com.dhoon.transfertracker.internal.transferpost.domain.TransferPost;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.transferpost.domain.TranslateStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransferPostRepository extends JpaRepository<TransferPost, Long> {

    Optional<TransferPost> findByExternalPostId(String externalPostId);

    List<TransferPost> findAllBySourcer(TransferPostSource sourcer);

    List<TransferPost> findAllByTeamId(Long teamId);

    List<TransferPost> findTop400ByTranslateStatusOrderByContentCreatedAtDescIdDesc(TranslateStatus translateStatus);
}
