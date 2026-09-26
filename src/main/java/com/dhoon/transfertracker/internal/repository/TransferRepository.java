package com.dhoon.transfertracker.internal.repository;

import com.dhoon.transfertracker.internal.domain.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TransferRepository extends JpaRepository<Transfer, Long> {

    @Query("""
            select t from Transfer t 
                        join fetch t.player 
                        join fetch t.inTeam
                        join fetch t.outTeam
            where t.player.id = :playerId
            """)
    List<Transfer> findAllByPlayerId(Long playerId);


    @Query("""
            select t from Transfer t 
                        join fetch t.player 
                        join fetch t.inTeam
                        join fetch t.outTeam
            """)
    List<Transfer> findAllTransferWithLazyEntities();



    // 전부 내부 API 아이디 ( 외부 전용 API 아이디 아님 )
    boolean existsByInTeamIdAndOutTeamIdAndPlayerId(Long inTeamId, Long outTeamId, Long playerId);

    Optional<Transfer> findByInTeamIdAndOutTeamIdAndPlayerId(Long inTeamId, Long outTeamId, Long playerId);
}
