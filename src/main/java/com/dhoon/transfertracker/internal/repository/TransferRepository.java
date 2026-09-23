package com.dhoon.transfertracker.internal.repository;

import com.dhoon.transfertracker.internal.domain.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransferRepository extends JpaRepository<Transfer, Long> {

    List<Transfer> findAllByPlayerId(Long playerId);
}
