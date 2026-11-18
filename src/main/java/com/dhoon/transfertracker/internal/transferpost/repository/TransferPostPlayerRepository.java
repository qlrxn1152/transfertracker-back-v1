package com.dhoon.transfertracker.internal.transferpost.repository;

import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostPlayer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransferPostPlayerRepository extends JpaRepository<TransferPostPlayer, Long> {

    Optional<TransferPostPlayer> findByPlayerIdAndTransferPostId(Long playerId, Long transferPostId);
}
