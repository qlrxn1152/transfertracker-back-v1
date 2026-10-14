package com.dhoon.transfertracker.internal.transferpost.repository;

import com.dhoon.transfertracker.internal.transferpost.domain.TransferPost;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.transferpost.domain.TranslateStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TransferPostRepository extends JpaRepository<TransferPost, Long> {

    @Query("""
            select p from TransferPost p
                        left join fetch p.team
            where p.id = :postId
            """)
    Optional<TransferPost> findByIdWithTeam(Long postId);

    Optional<TransferPost> findByExternalPostId(String externalPostId);

    Optional<TransferPost> findBySourcerAndContent(TransferPostSource sourcer, String content);

    List<TransferPost> findAllBySourcer(TransferPostSource sourcer);

    List<TransferPost> findAllByTeamId(Long teamId);

    @Query("""
            select p from TransferPost p
                        join fetch p.team t
            where t.id in :teamIds
            order by p.contentCreatedAt desc, p.id desc
            """)
    List<TransferPost> findAllNotificationCandidatesByTeamIds(List<Long> teamIds);

    @Query("""
            select p from TransferPost p
                        join fetch p.team t
            where t.id in :teamIds
              and p.transferRelated = true
            order by p.contentCreatedAt desc, p.id desc
            """)
    List<TransferPost> findRecentTransferRelatedByTeamIds(List<Long> teamIds, Pageable pageable);

    List<TransferPost> findTop20ByTranslateStatusOrderByContentCreatedAtDescIdDesc(TranslateStatus translateStatus);

    List<TransferPost> findAllByTeamIsNull();

    long countByContentCreatedAtBetween(Instant startInclusive, Instant endExclusive);

    long countByTranslateStatus(TranslateStatus translateStatus);

    long countByTeamIsNull();

    long countBySourcer(TransferPostSource sourcer);
}
