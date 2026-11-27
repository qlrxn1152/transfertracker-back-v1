package com.dhoon.transfertracker.internal.fixture.controller;
import com.dhoon.transfertracker.internal.fixture.dto.record.FixturePlayerStatisticsResponse;
import com.dhoon.transfertracker.internal.fixture.dto.record.FixtureStatisticsResponse;
import com.dhoon.transfertracker.internal.fixture.service.FixtureStatisticsQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/fixtures")
public class FixtureStatisticsController {
    private final FixtureStatisticsQueryService queryService;
    @GetMapping("/{fixtureId}/statistics")
    public List<FixtureStatisticsResponse> getStatistics(@PathVariable("fixtureId") Long fixtureId) { return queryService.getStatistics(fixtureId); }
    @GetMapping("/{fixtureId}/player-statistics")
    public List<FixturePlayerStatisticsResponse> getPlayerStatistics(@PathVariable("fixtureId") Long fixtureId) { return queryService.getPlayerStatistics(fixtureId); }
}
