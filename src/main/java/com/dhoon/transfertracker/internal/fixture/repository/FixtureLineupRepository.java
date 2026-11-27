package com.dhoon.transfertracker.internal.fixture.repository;

import com.dhoon.transfertracker.internal.fixture.domain.FixtureLineup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FixtureLineupRepository
        extends JpaRepository<FixtureLineup, Long> {

    Optional<FixtureLineup> findByFixtureIdAndTeamId(Long fixtureId, Long teamId);

    @Query("""
            SELECT fl
            FROM FixtureLineup fl
            JOIN FETCH fl.team
            WHERE fl.fixture.id = :fixtureId
            """)
    List<FixtureLineup> findAllByFixtureId(Long fixtureId);
}