package com.dhoon.transfertracker.internal.teamplayer.repository;

import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.teamplayer.domain.TeamPlayer;
import com.dhoon.transfertracker.internal.transfer.domain.Transfer;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TeamPlayerRepository extends JpaRepository<TeamPlayer, Long> {

    @Query("select tp from TeamPlayer tp join fetch tp.player where tp.team.id = :teamId")
    List<TeamPlayer> findAllByTeamIdWithLazyEntity(Long teamId);

    @Query("select tp from TeamPlayer tp join fetch tp.player join fetch tp.team where tp.player.id = :playerId and tp.team.id = :teamId")
    Optional<TeamPlayer> findByPlayerIdAndTeamId(Long playerId, Long teamId);

    @Query("select tp from TeamPlayer tp join fetch tp.team where tp.player.id = :playerId")
    Optional<TeamPlayer> findByPlayerId(Long playerId);

    long countByTeamId(Long teamId);

    boolean existsByPlayerId(Long playerId);


    @Query("""
            select tp from TeamPlayer tp
                        join fetch tp.player p
                        join fetch tp.team t
            where (
                   :leagueCode is null
                               or t.leagueCode = :leagueCode
                   )
            and (
                   :keyWord = ''
                               or lower(p.playerName) like lower(concat('%', :keyWord, '%'))
                   )
            and (
                   :teamId is null
                   or t.id = :teamId
                )
            """)
    Slice<TeamPlayer> findTeamPlayers(String keyWord, Pageable pageable, LeagueCode leagueCode, Long teamId);

    @Query("""
            select tp from TeamPlayer tp
                        join fetch tp.player p
                        join fetch tp.team t
            where p.playerNameKo is null
                and t.leagueCode is not null
            order by p.id asc
            """)
    List<TeamPlayer> findPlayerNameTranslationTargets(Pageable pageable);






}
