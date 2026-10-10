package com.dhoon.transfertracker.internal.team.repository;

import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.Team;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {

    boolean existsByApiFootballId(Long apiFootballId);

    Optional<Team> findByApiFootballId(Long apiFootballId);

    List<Team> findAllByLeagueCode(LeagueCode leagueCode);

    Optional<Team> findByTeamName(String teamName);

    @Query("select t from Team t where t.leagueCode is not null")
    Slice<Team> findAllByLeagueCodeIsNotNullWithPage(Pageable pageable);

    List<Team> findAllByLeagueCodeIsNotNull();

    List<Team> findAllByLeagueCodeIsNull();
}
