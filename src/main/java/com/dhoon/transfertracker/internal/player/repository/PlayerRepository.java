package com.dhoon.transfertracker.internal.player.repository;

import com.dhoon.transfertracker.internal.player.domain.Player;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    Optional<Player> findByApiFootballId(Long apiFootballId);

    Slice<Player> findAllBy(Pageable pageable);

    Slice<Player> findByPlayerNameContainingIgnoreCase(String keyWord, Pageable pageable);

    boolean existsByApiFootballId(Long apiFootballId);

    @Query("""
            select p from Player p
            where p.playerNameKo is null
                or trim(p.playerNameKo) = ''
            order by p.id asc
            """)
    List<Player> findPlayerNameTranslationTargets(Pageable pageable);

    Optional<Player> findByPlayerName(String playerName);

    @Query("""
            select p from Player p
            where lower(p.playerName) = lower(:playerName)
               or lower(p.playerNameKo) = lower(:playerName)
            """)
    List<Player> findAllByAnyDisplayName(@Param("playerName") String playerName);

    List<Player> findAllByApiFootballIdIn(Collection<Long> apiFootballIds);









}
