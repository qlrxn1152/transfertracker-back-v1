package com.dhoon.transfertracker.internal.repository;

import com.dhoon.transfertracker.internal.domain.Player;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRepository extends JpaRepository<Player, Long> {
}
