package com.dhoon.transfertracker.internal.player.repository;

import com.dhoon.transfertracker.internal.player.domain.Player;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    Optional<Player> findByApiFootballId(Long apiFootballId);

    Slice<Player> findAllBy(Pageable pageable);

    boolean existsByApiFootballId(Long apiFootballId);
}
