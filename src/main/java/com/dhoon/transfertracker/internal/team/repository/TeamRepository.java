package com.dhoon.transfertracker.internal.team.repository;

import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.domain.TeamCollectionLeague;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {

    // API ID 기준
    Optional<Team> findByApiFootballId(Long apiFootballId);
    List<Team> findAllByApiFootballIdIn(Collection<Long> apiFootballIds);

    // 리그 기반
    List<Team> findAllByLeagueCode(LeagueCode leagueCode);
    List<Team> findAllByLeagueCodeIsNotNull();
    List<Team> findAllByCollectionLeagueCode(TeamCollectionLeague collectionLeagueCode);
    List<Team> findAllByCollectionLeagueCodeIsNotNull();


    // 이름 기반
    @Query("""
            select t from Team t
            where lower(t.teamName) = lower(:teamName)
               or lower(t.teamNameKo) = lower(:teamName)
            """)
    List<Team> findAllByAnyDisplayName(String teamName);



}
