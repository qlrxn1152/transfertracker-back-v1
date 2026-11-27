package com.dhoon.transfertracker.internal.fixture.service;

import com.dhoon.transfertracker.internal.fixture.domain.Fixture;
import com.dhoon.transfertracker.internal.fixture.domain.FixtureStatus;
import com.dhoon.transfertracker.internal.fixture.dto.record.FixtureResponse;
import com.dhoon.transfertracker.internal.fixture.exception.NotFoundFixtureException;
import com.dhoon.transfertracker.internal.fixture.repository.FixtureRepository;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.exception.NotFoundTeamException;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FixtureQueryService {

    private final FixtureRepository fixtureRepository;
    private final TeamRepository teamRepository;

    private static final List<FixtureStatus> LIVE_STATUSES =
            List.of(
                    FixtureStatus.FIRST_HALF,
                    FixtureStatus.HALF_TIME,
                    FixtureStatus.SECOND_HALF,
                    FixtureStatus.EXTRA_TIME,
                    FixtureStatus.BREAK_TIME,
                    FixtureStatus.PENALTY_IN_PROGRESS,
                    FixtureStatus.LIVE
            );

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    public List<FixtureResponse> getFixturesByDate(LocalDate date, LeagueCode leagueCode) {

        Instant startAt = date.atStartOfDay(KST).toInstant();
        Instant endAt = date.plusDays(1).atStartOfDay(KST).toInstant();

        if (leagueCode != null) {
            return fixtureRepository.findAllByKickoffAtRangeAndLeagueCode(startAt, endAt, leagueCode)
                    .stream()
                    .map(FixtureResponse::from)
                    .toList();
        }

        return fixtureRepository.findAllByKickoffAtRange(startAt, endAt)
                .stream()
                .map(FixtureResponse::from)
                .toList();
    }

    public Page<FixtureResponse> getTeamFixtures(Long teamId, int page) {
        teamRepository.findById(teamId).orElseThrow(NotFoundTeamException::new);

        PageRequest pageable = PageRequest.of(
                page,
                10
        );

        Page<Fixture> teamFixtures = fixtureRepository.findAllByTeamId(teamId, pageable);

        return teamFixtures.map(FixtureResponse::from);
    }

    public FixtureResponse getFixture(Long fixtureId) {
        Fixture fixture = fixtureRepository.findByFixtureIdWithLazy(fixtureId)
                .orElseThrow(NotFoundFixtureException::new);

        return FixtureResponse.from(fixture);
    }

    public List<FixtureResponse> getTeamUpcomingFixtures(Long teamId) {
        teamRepository.findById(teamId).orElseThrow(NotFoundTeamException::new);

        return fixtureRepository.findTeamUpcomingFixtures(teamId, Instant.now(), FixtureStatus.NOT_STARTED)
                .stream()
                .map(FixtureResponse::from)
                .toList();
    }

    public List<FixtureResponse> getTeamRecentFixtures(Long teamId) {
        teamRepository.findById(teamId).orElseThrow(NotFoundTeamException::new);

        List<FixtureStatus> statuses = List.of(
                FixtureStatus.FINISHED,
                FixtureStatus.AFTER_EXTRA_TIME,
                FixtureStatus.AFTER_PENALTY
        );

        return fixtureRepository.findTeamRecentFixtures(teamId, statuses)
                .stream()
                .map(FixtureResponse::from)
                .toList();
    }

    public List<FixtureResponse> getLiveFixtures() {
        return fixtureRepository.findLiveFixtures(LIVE_STATUSES)
                .stream()
                .map(FixtureResponse::from)
                .toList();
    }
}
