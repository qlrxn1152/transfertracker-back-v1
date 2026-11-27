package com.dhoon.transfertracker.internal.fixture.repository;

import com.dhoon.transfertracker.internal.fixture.domain.FixtureEvent;
import com.dhoon.transfertracker.internal.fixture.domain.FixtureEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FixtureEventRepository extends JpaRepository<FixtureEvent, Long> {

    @Modifying
    @Query("""
    DELETE FROM FixtureEvent fe
    WHERE fe.fixture.id = :fixtureId
    """)
    void deleteAllByFixtureId(Long fixtureId);

    @Query("""
    SELECT fe FROM FixtureEvent fe
    JOIN FETCH fe.team
    LEFT JOIN FETCH fe.player
    LEFT JOIN FETCH fe.relatedPlayer
    WHERE fe.fixture.id = :fixtureId
    ORDER BY fe.elapsed ASC, fe.extra ASC, fe.id ASC
    """)
    List<FixtureEvent> findAllByFixtureId(Long fixtureId);

}
