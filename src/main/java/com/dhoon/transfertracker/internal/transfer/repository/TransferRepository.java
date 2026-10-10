package com.dhoon.transfertracker.internal.transfer.repository;

import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.transfer.domain.Transfer;
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
    Slice<Transfer> findAllTransferWithLazyEntities(Pageable pageable);

    @Query("""
            select t from Transfer t 
                        join fetch t.player 
                        join fetch t.inTeam
                        join fetch t.outTeam
            """)
    List<Transfer> findAllWithLazyEntities();


    @Query("""
            select t from Transfer t 
                        join fetch t.player p
                        join fetch t.inTeam
                        join fetch t.outTeam
            where lower(p.playerName) like lower(concat('%', :keyWord, '%'))                        
            """)
    Slice<Transfer> findByPlayerNameContainingIgnoreCaseWithLazyEntities(String keyWord, Pageable pageable);

    Optional<Transfer> findByInTeamIdAndOutTeamIdAndPlayerIdAndTransferDate(Long inTeamId, Long outTeamId, Long playerId, LocalDate transferDate);





    @Query("""
            select t from Transfer t 
                        join fetch t.player p
                        join fetch t.inTeam it
                        join fetch t.outTeam ot
            where ((t.inTeam.id = :teamId) or (t.outTeam.id = :teamId))
            """)
    Slice<Transfer> findAllByTeamIdWithLazy(Long teamId, Pageable pageable);


    @Query("""
            select t from Transfer t 
                        join fetch t.player p
                        join fetch t.inTeam
                        join fetch t.outTeam
            where ( (t.inTeam.id = :teamId) or (t.outTeam.id = :teamId) ) and (lower(p.playerName) like lower(concat('%', :keyWord, '%')))                         
            """)
    Slice<Transfer> findAllByTeamIdWithLazyAndKeyWord(Long teamId, String keyWord, Pageable pageable);



    @Query("""
            select t from Transfer t 
                        join fetch t.player p
                        join fetch t.inTeam it
                        join fetch t.outTeam ot
            where (
                   :leagueCode is null
                               or it.leagueCode = :leagueCode
                               or ot.leagueCode = :leagueCode
                   )            
            and (
                   :keyWord = ''
                               or lower(p.playerName) like lower(concat('%', :keyWord, '%'))
                   )   
            and (
                   :teamId is null
                               or it.id = :teamId
                               or ot.id = :teamId            
                   )                   
            """)
    Slice<Transfer> findTransfers(String keyWord, Pageable pageable, LeagueCode leagueCode, Long teamId);
}
