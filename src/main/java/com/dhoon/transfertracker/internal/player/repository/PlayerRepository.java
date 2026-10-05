package com.dhoon.transfertracker.internal.player.repository;

import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.transfer.domain.Transfer;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPost;
import com.dhoon.transfertracker.internal.transferpost.domain.TranslateStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    Optional<Player> findByApiFootballId(Long apiFootballId);

    Slice<Player> findAllBy(Pageable pageable);

    Slice<Player> findByPlayerNameContainingIgnoreCase(String keyWord, Pageable pageable);

    boolean existsByApiFootballId(Long apiFootballId);








}
