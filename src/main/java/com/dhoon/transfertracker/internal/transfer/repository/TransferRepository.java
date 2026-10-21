package com.dhoon.transfertracker.internal.transfer.repository;

import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.transfer.domain.Transfer;
import com.dhoon.transfertracker.internal.transfer.query.projection.TransferSearchProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransferRepository extends JpaRepository<Transfer, Long> {

    @Query("""
            select t from Transfer t
                        join fetch t.player
                        join fetch t.inTeam
                        join fetch t.outTeam
            where t.id = :transferId
            """)
    Optional<Transfer> findByIdWithLazyEntities(Long transferId);

    @Query("""
            select t from Transfer t 
                        join fetch t.player 
                        join fetch t.inTeam
                        join fetch t.outTeam
            where t.player.id = :playerId
            order by t.transferDate desc, t.id desc
            """)
    List<Transfer> findAllByPlayerId(Long playerId);


    @Query("""
            select t from Transfer t 
                        join fetch t.player 
                        join fetch t.inTeam
                        join fetch t.outTeam
            """)
    List<Transfer> findAllWithLazyEntities();


    Optional<Transfer> findByInTeamIdAndOutTeamIdAndPlayerIdAndTransferDate(Long inTeamId, Long outTeamId, Long playerId, LocalDate transferDate);

    @Query("""
            select t from Transfer t 
                        join fetch t.player p
                        join fetch t.inTeam it
                        join fetch t.outTeam ot
            where it.id in :teamIds
               or ot.id in :teamIds
            order by t.transferDate desc, t.id desc
            """)
    List<Transfer> findRecentByTeamIds(List<Long> teamIds, Pageable pageable);



}
