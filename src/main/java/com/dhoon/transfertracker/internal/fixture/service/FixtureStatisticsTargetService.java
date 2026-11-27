package com.dhoon.transfertracker.internal.fixture.service;
import com.dhoon.transfertracker.internal.fixture.domain.*;
import com.dhoon.transfertracker.internal.fixture.repository.FixtureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.List;
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FixtureStatisticsTargetService {
    private final FixtureRepository fixtureRepository;
    private static final List<FixtureStatus> FINAL = List.of(FixtureStatus.FINISHED, FixtureStatus.AFTER_EXTRA_TIME, FixtureStatus.AFTER_PENALTY);
    public List<Long> getTargets(boolean players) {
        Instant from = Instant.now().minus(Duration.ofDays(2));
        List<Fixture> fixtures = players ? fixtureRepository.findFixturesToSyncPlayerStatistics(FINAL, from, PageRequest.of(0, 20)) : fixtureRepository.findFixturesToSyncStatistics(FINAL, from, PageRequest.of(0, 20));
        return fixtures.stream().map(Fixture::getApiFootballId).toList();
    }
}
