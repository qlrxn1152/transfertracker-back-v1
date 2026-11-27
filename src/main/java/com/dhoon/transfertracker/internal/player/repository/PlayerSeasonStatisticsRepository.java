package com.dhoon.transfertracker.internal.player.repository;
import com.dhoon.transfertracker.internal.player.domain.PlayerSeasonStatistics;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
public interface PlayerSeasonStatisticsRepository extends JpaRepository<PlayerSeasonStatistics, Long> {
    @Query("SELECT e FROM PlayerSeasonStatistics e JOIN FETCH e.team JOIN FETCH e.player WHERE e.player.id = :id ORDER BY e.season DESC, e.leagueCode, e.team.id")
    List<PlayerSeasonStatistics> findAllByPlayerId(@Param("id") Long id);

    @Query("SELECT e FROM PlayerSeasonStatistics e JOIN FETCH e.team JOIN FETCH e.player WHERE e.player.apiFootballId = :apiId AND e.leagueCode = :league AND e.season = :season")
    List<PlayerSeasonStatistics> findExisting(@Param("apiId") Long apiId, @Param("league") LeagueCode league, @Param("season") Integer season);

}
