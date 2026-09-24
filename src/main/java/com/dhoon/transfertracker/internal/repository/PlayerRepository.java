package com.dhoon.transfertracker.internal.repository;

import com.dhoon.transfertracker.internal.domain.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    Optional<Player> findByApiFootballId(Long apiFootballId);
}
