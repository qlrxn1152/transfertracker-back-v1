package com.dhoon.transfertracker.internal.transferpost.repository;

import com.dhoon.transfertracker.internal.transferpost.domain.TransferPost;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.transferpost.domain.TranslateStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

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



    @Query(
            value = """
        SELECT tp
        FROM TransferPost tp
        LEFT JOIN FETCH tp.team
        WHERE tp.sourcer = :sourcer
        """,
            countQuery = """
        SELECT COUNT(tp)
        FROM TransferPost tp
        WHERE tp.sourcer = :sourcer
        """
    )
    Page<TransferPost> findAllBySourcerWithPageAndLazyTeam(TransferPostSource sourcer, Pageable pageable);




    List<TransferPost> findAllByTeamId(Long teamId);


    List<TransferPost> findTop20ByTranslateStatusOrderByContentCreatedAtDescIdDesc(TranslateStatus translateStatus);

    List<TransferPost> findAllByTeamIsNull();

}
