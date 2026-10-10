package com.dhoon.transfertracker.internal.transferpost.repository;

import com.dhoon.transfertracker.internal.transferpost.domain.TransferPost;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.transferpost.domain.TranslateStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TransferPostRepository extends JpaRepository<TransferPost, Long> {

    Optional<TransferPost> findByExternalPostId(String externalPostId);

    List<TransferPost> findAllBySourcer(TransferPostSource sourcer);

    List<TransferPost> findAllByTeamId(Long teamId);

    List<TransferPost> findTop20ByTranslateStatusOrderByContentCreatedAtDescIdDesc(TranslateStatus translateStatus);

    List<TransferPost> findAllByTeamIsNull();

    long countByContentCreatedAtBetween(Instant startInclusive, Instant endExclusive);

    long countByTranslateStatus(TranslateStatus translateStatus);

    long countByTeamIsNull();

    long countBySourcer(TransferPostSource sourcer);
}
