package com.dhoon.transfertracker.internal.player.repository;

import com.dhoon.transfertracker.internal.player.domain.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    Optional<Player> findByApiFootballId(Long apiFootballId);

    boolean existsByApiFootballId(Long apiFootballId);
}
