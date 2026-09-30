package com.dhoon.transfertracker.internal.teamplayer.repository;

import com.dhoon.transfertracker.internal.teamplayer.domain.TeamPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TeamPlayerRepository extends JpaRepository<TeamPlayer, Long> {

    @Query("select tp from TeamPlayer tp join fetch tp.player where tp.team.id = :teamId")
    List<TeamPlayer> findAllByTeamIdWithLazyEntity(Long teamId);

    Optional<TeamPlayer> findByPlayerIdAndTeamId(Long playerId, Long teamId);

    long countByTeamId(Long teamId);
}
