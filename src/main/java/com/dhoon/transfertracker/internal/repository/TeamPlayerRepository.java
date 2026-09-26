package com.dhoon.transfertracker.internal.repository;

import com.dhoon.transfertracker.internal.domain.TeamPlayer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamPlayerRepository extends JpaRepository<TeamPlayer, Long> {

    List<TeamPlayer> findAllByTeamId(Long teamId);

    Optional<TeamPlayer> findByPlayerIdAndTeamId(Long playerId, Long teamId);
}
