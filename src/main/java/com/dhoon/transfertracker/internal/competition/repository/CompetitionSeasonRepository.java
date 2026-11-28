package com.dhoon.transfertracker.internal.competition.repository;

import com.dhoon.transfertracker.internal.competition.domain.CompetitionSeason;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompetitionSeasonRepository
        extends JpaRepository<CompetitionSeason, Long> {

    List<CompetitionSeason> findAllByLeagueCode(
            LeagueCode leagueCode
    );

    Optional<CompetitionSeason> findByLeagueCodeAndSeason(
            LeagueCode leagueCode,
            Integer season
    );

    List<CompetitionSeason> findAllByCurrentSeasonTrue();

    Optional<CompetitionSeason>
    findFirstByLeagueCodeAndCurrentSeasonTrueOrderBySeasonDesc(
            LeagueCode leagueCode
    );

    List<CompetitionSeason> findAllByLeagueCodeOrderBySeasonDesc(
            LeagueCode leagueCode
    );
}