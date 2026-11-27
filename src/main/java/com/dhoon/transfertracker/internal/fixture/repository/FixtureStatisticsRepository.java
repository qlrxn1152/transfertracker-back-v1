package com.dhoon.transfertracker.internal.fixture.repository;
import com.dhoon.transfertracker.internal.fixture.domain.FixtureStatistics;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
public interface FixtureStatisticsRepository extends JpaRepository<FixtureStatistics, Long> {
    @Query("SELECT e FROM FixtureStatistics e JOIN FETCH e.team WHERE e.fixture.id = :id")
    List<FixtureStatistics> findAllByFixtureId(@Param("id") Long id);

}
