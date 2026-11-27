package com.dhoon.transfertracker.internal.fixture.service;
import com.dhoon.transfertracker.internal.fixture.dto.record.*;
import com.dhoon.transfertracker.internal.fixture.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FixtureStatisticsQueryService {
    private final FixtureStatisticsRepository fixtureStatisticsRepository;
    private final FixturePlayerStatisticsRepository fixturePlayerStatisticsRepository;
    public List<FixtureStatisticsResponse> getStatistics(Long fixtureId) { return fixtureStatisticsRepository.findAllByFixtureId(fixtureId).stream().map(FixtureStatisticsResponse::from).toList(); }
    public List<FixturePlayerStatisticsResponse> getPlayerStatistics(Long fixtureId) { return fixturePlayerStatisticsRepository.findAllByFixtureId(fixtureId).stream().map(FixturePlayerStatisticsResponse::from).toList(); }
}
