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
               or lower(coalesce(p.playerNameKo, '')) like lower(concat('%', :keyWord, '%'))
            """)
    Slice<Transfer> findByPlayerNameContainingIgnoreCaseWithLazyEntities(String keyWord, Pageable pageable);

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
            where (
                   (t.inTeam.id = :teamId)
                               or (t.outTeam.id = :teamId)
                   )
              and (
                   lower(p.playerName) like lower(concat('%', :keyWord, '%'))
                               or lower(coalesce(p.playerNameKo, '')) like lower(concat('%', :keyWord, '%'))
                   )
            """)
    Slice<Transfer> findAllByTeamIdWithLazyAndKeyWord(Long teamId, String keyWord, Pageable pageable);



    @Query(
            value = """
                SELECT
                    t.transfer_id AS transferId,

                    p.player_id AS playerId,
                    COALESCE(
                        NULLIF(TRIM(p.player_name_ko), ''),
                        p.player_name
                    ) AS playerName,
                    p.api_football_id AS playerApiFootballId,

                    it.team_id AS inTeamId,
                    COALESCE(
                        NULLIF(it.team_name_ko, ''),
                        it.team_name
                    ) AS inTeamName,

                    ot.team_id AS outTeamId,
                    COALESCE(
                        NULLIF(ot.team_name_ko, ''),
                        ot.team_name
                    ) AS outTeamName,

                    t.transfer_date AS transferDate,
                    t.transfer_type AS transferType

                FROM transfer t

                JOIN player p
                    ON p.player_id = t.player_id

                JOIN team it
                    ON it.team_id = t.in_team_id

                JOIN team ot
                    ON ot.team_id = t.out_team_id

                WHERE MATCH(p.player_name, p.player_name_ko)
                      AGAINST (:keyWord IN BOOLEAN MODE)

                AND (
                    :leagueCode IS NULL
                    OR it.league_api_code = :leagueCode
                    OR ot.league_api_code = :leagueCode
                )

                AND (
                    :teamId IS NULL
                    OR it.team_id = :teamId
                    OR ot.team_id = :teamId
                )

                ORDER BY
                    t.transfer_date DESC,
                    t.transfer_id DESC
                """,
            nativeQuery = true
    )
    Slice<TransferSearchProjection> findTransfersByFullText(
            String keyWord,
            String leagueCode,
            Long teamId,
            Pageable pageable
    );



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
                   :teamId is null
                               or it.id = :teamId
                               or ot.id = :teamId            
                   )                   
            """)
    Slice<Transfer> findTransfersWithoutKeyword(Pageable pageable, LeagueCode leagueCode, Long teamId);
}
