package com.dhoon.transfertracker.internal.transferpost.repository;

import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TransferPostTeamRepository extends JpaRepository<TransferPostTeam, Long> {

    @Query("""
            select tpt from TransferPostTeam tpt
            join fetch tpt.team
            WHERE tpt.team.id = :teamId
            """)
    List<TransferPostTeam> findAllByTeamId(Long teamId);

    boolean existsByPostId(Long postId);

}
