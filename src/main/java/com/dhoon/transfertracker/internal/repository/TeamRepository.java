package com.dhoon.transfertracker.internal.repository;

import com.dhoon.transfertracker.internal.domain.LeagueCode;
import com.dhoon.transfertracker.internal.domain.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {

    boolean existsByApiFootballId(Long apiFootballId);

    Optional<Team> findByApiFootballId(Long apiFootballId);

    List<Team> findAllByLeagueCode(LeagueCode leagueCode);


}
