package com.dhoon.transfertracker.internal.standing.repository;

import com.dhoon.transfertracker.internal.standing.domain.LeagueStanding;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LeagueStandingRepository
        extends JpaRepository<LeagueStanding, Long> {

    @Query("""
            SELECT ls
            FROM LeagueStanding ls
            JOIN FETCH ls.team
            WHERE ls.leagueCode = :leagueCode
              AND ls.season = :season
            ORDER BY ls.groupName ASC, ls.ranking ASC
            """)
    List<LeagueStanding> findAllByLeagueCodeAndSeason(
            @Param("leagueCode") LeagueCode leagueCode,
            @Param("season") Integer season
    );
}