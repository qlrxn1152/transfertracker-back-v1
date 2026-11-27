package com.dhoon.transfertracker.internal.fixture.service;
import com.dhoon.transfertracker.internal.fixture.domain.*;
import com.dhoon.transfertracker.internal.fixture.dto.record.ApiFootballFixtureStatisticsResponse;
import com.dhoon.transfertracker.internal.fixture.repository.*;
import com.dhoon.transfertracker.internal.team.domain.Team;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
@Service
@Transactional
@RequiredArgsConstructor
public class FixtureStatisticsSyncTxService {
    private final FixtureRepository fixtureRepository;
    private final FixtureStatisticsRepository repository;
    public void syncFixtureStatistics(Long fixtureApiId, List<ApiFootballFixtureStatisticsResponse> records) {
        Fixture fixture = fixtureRepository.findByApiFootballId(fixtureApiId).orElseThrow();
        Map<Long, Team> teams = Map.of(fixture.getHomeTeam().getApiFootballId(), fixture.getHomeTeam(), fixture.getAwayTeam().getApiFootballId(), fixture.getAwayTeam());
        if (!records.stream().map(ApiFootballFixtureStatisticsResponse::teamApiId).collect(Collectors.toSet()).equals(teams.keySet())) throw new IllegalArgumentException("Invalid fixture teams");
        Map<String, FixtureStatistics> existing = repository.findAllByFixtureId(fixture.getId()).stream().collect(Collectors.toMap(e -> e.getTeam().getId().toString(), Function.identity()));
        Set<String> seen = new HashSet<>();
        List<FixtureStatistics> created = new ArrayList<>();
        for (ApiFootballFixtureStatisticsResponse r : records) {
            Team team = teams.get(r.teamApiId());
            String key = team.getId().toString();
            if (!seen.add(key)) throw new IllegalArgumentException("Duplicate statistic");
            FixtureStatistics entity = existing.get(key);
            if (entity == null) { entity = FixtureStatistics.of(fixture, team); created.add(entity); }
            entity.update(
                    r.passesAccurate(),
                    r.shotsInsideBox(),
                    r.shotsOutsideBox(),
                    r.blockedShots(),
                    r.freeKicks(),
                    r.shotsOnGoal(),
                    r.goalkeeperSaves(),
                    r.shotsOffGoal(),
                    r.totalPasses(),
                    r.totalShots(),
                    r.yellowCards(),
                    r.cornerKicks(),
                    r.offsides(),
                    r.fouls(),
                    r.redCards(),
                    r.ballPossession(),
                    r.expectedGoals()
            );
        }
        repository.saveAll(created);
        if (List.of(FixtureStatus.FINISHED, FixtureStatus.AFTER_EXTRA_TIME, FixtureStatus.AFTER_PENALTY).contains(fixture.getStatus())) fixture.completeStatisticsSync();
    }
}
