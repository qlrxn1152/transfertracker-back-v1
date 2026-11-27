package com.dhoon.transfertracker.internal.player.repository;
import com.dhoon.transfertracker.internal.player.domain.*;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
public interface PlayerRankingRepository extends JpaRepository<PlayerRanking, Long> {
    @Modifying
    @Query("DELETE FROM PlayerRanking r WHERE r.leagueCode = :league AND r.season = :season AND r.rankingType = :type")
    void deleteSnapshot(@Param("league") LeagueCode league, @Param("season") Integer season, @Param("type") PlayerRankingType type);
    @Query("SELECT r FROM PlayerRanking r JOIN FETCH r.player WHERE r.leagueCode = :league AND r.season = :season AND r.rankingType = :type ORDER BY r.ranking ASC")
    List<PlayerRanking> findSnapshot(@Param("league") LeagueCode league, @Param("season") Integer season, @Param("type") PlayerRankingType type);
}
