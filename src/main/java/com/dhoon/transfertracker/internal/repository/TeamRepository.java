package com.dhoon.transfertracker.internal.repository;

import com.dhoon.transfertracker.internal.domain.Team;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Long> {

    boolean existsByApiFootballId(Long apiFootballId);
}
