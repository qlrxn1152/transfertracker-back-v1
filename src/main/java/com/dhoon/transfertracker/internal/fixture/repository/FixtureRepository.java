package com.dhoon.transfertracker.internal.fixture.repository;

import com.dhoon.transfertracker.internal.fixture.domain.Fixture;
import com.dhoon.transfertracker.internal.fixture.domain.FixtureStatus;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FixtureRepository extends JpaRepository<Fixture, Long> {

    List<Fixture> findAllByApiFootballIdIn(Collection<Long> apiFootballIds);

    @Query("""
    SELECT f FROM Fixture f
    JOIN FETCH f.homeTeam
    JOIN FETCH f.awayTeam
    WHERE f.kickoffAt >= :startAt
    AND f.kickoffAt < :endAt
    ORDER BY f.kickoffAt ASC
    """)
    List<Fixture> findAllByKickoffAtRange(Instant startAt, Instant endAt);


    @Query("""
    SELECT f FROM Fixture f
    JOIN FETCH f.homeTeam
    JOIN FETCH f.awayTeam
    WHERE f.kickoffAt >= :startAt
    AND f.kickoffAt < :endAt
    AND f.leagueCode = :leagueCode
    ORDER BY f.kickoffAt ASC
    """)
    List<Fixture> findAllByKickoffAtRangeAndLeagueCode(Instant startAt, Instant endAt, LeagueCode leagueCode);

    @Query(
            value = """
        SELECT f FROM Fixture f
        JOIN FETCH f.homeTeam ht
        JOIN FETCH f.awayTeam at
        WHERE (ht.id = :teamId OR at.id = :teamId)
        ORDER BY f.kickoffAt ASC, f.id ASC
        """,
            countQuery = """
        SELECT COUNT(f) FROM Fixture f
        WHERE (f.homeTeam.id = :teamId OR f.awayTeam.id = :teamId)
        """
    )
    Page<Fixture> findAllByTeamId(Long teamId, Pageable pageable);

    @Query("""
    SELECT f FROM Fixture f
    JOIN FETCH f.homeTeam
    JOIN FETCH f.awayTeam
    WHERE f.id = :fixtureId
    """)
    Optional<Fixture> findByFixtureIdWithLazy(Long fixtureId);

    @Query("""
    SELECT f FROM Fixture f
    JOIN FETCH f.homeTeam ht
    JOIN FETCH f.awayTeam at
    WHERE (ht.id = :teamId OR at.id = :teamId)
    AND f.kickoffAt >= :now
    AND f.status = :status
    ORDER BY f.kickoffAt ASC, f.id ASC
    """)
    List<Fixture> findTeamUpcomingFixtures(Long teamId, Instant now, FixtureStatus status);

    @Query("""
    SELECT f FROM Fixture f
    JOIN FETCH f.homeTeam ht
    JOIN FETCH f.awayTeam at
    WHERE (ht.id = :teamId OR at.id = :teamId)
    AND f.status IN :statuses
    ORDER BY f.kickoffAt DESC, f.id DESC
    """)
    List<Fixture> findTeamRecentFixtures(Long teamId, Collection<FixtureStatus> statuses);

}
