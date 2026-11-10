package com.dhoon.transfertracker.internal.transferpost.repository;

import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostTeam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TransferPostTeamRepository extends JpaRepository<TransferPostTeam, Long> {

    @Query("""
            select tpt from TransferPostTeam tpt
            join fetch tpt.team
            WHERE tpt.team.id = :teamId
            """)
    Page<TransferPostTeam> findAllByTeamId(Long teamId, Pageable pageable);

    boolean existsByPostId(Long postId);

    List<TransferPostTeam> findAllByPostId(Long postId);



}
