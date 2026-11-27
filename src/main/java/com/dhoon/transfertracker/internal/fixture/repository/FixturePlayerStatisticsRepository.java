package com.dhoon.transfertracker.internal.fixture.repository;
import com.dhoon.transfertracker.internal.fixture.domain.FixturePlayerStatistics;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
public interface FixturePlayerStatisticsRepository extends JpaRepository<FixturePlayerStatistics, Long> {
    @Query("SELECT e FROM FixturePlayerStatistics e JOIN FETCH e.team JOIN FETCH e.player WHERE e.fixture.id = :id")
    List<FixturePlayerStatistics> findAllByFixtureId(@Param("id") Long id);

}
