package com.dhoon.transfertracker.internal.fixture.repository;

import com.dhoon.transfertracker.internal.fixture.domain.FixtureLineupPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FixtureLineupPlayerRepository
        extends JpaRepository<FixtureLineupPlayer, Long> {

    @Modifying
    @Query("""
            DELETE FROM FixtureLineupPlayer flp
            WHERE flp.fixtureLineup.id = :fixtureLineupId
            """)
    void deleteAllByFixtureLineupId(Long fixtureLineupId);

    @Query("""
            SELECT flp
            FROM FixtureLineupPlayer flp
            JOIN FETCH flp.player
            JOIN FETCH flp.fixtureLineup fl
            WHERE fl.fixture.id = :fixtureId
            ORDER BY flp.id ASC
            """)
    List<FixtureLineupPlayer> findAllByFixtureId(Long fixtureId);
}