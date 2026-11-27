package com.dhoon.transfertracker.internal.fixture.service;

import com.dhoon.transfertracker.external.football.dto.ApiFootballFixtureResponse;
import com.dhoon.transfertracker.internal.fixture.domain.Fixture;
import com.dhoon.transfertracker.internal.fixture.domain.FixtureStatus;
import com.dhoon.transfertracker.internal.fixture.exception.AlreadyExistFixtureException;
import com.dhoon.transfertracker.internal.fixture.repository.FixtureRepository;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.exception.NotFoundTeamException;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Transactional
@Service
public class FixtureSyncTxService {

    private final FixtureRepository fixtureRepository;

    private final TeamRepository teamRepository;

    public void syncFixtures(List<ApiFootballFixtureResponse> fixtures) {

        if ( fixtures.isEmpty() ) {
            log.info("업데이트할 매치가 존재하지 않습니다. 작업을 종료합니다");
            return;
        }

        Set<Long> teamApiIds = new HashSet<>();
        Set<Long> apiFootballIds = new HashSet<>();

        for (ApiFootballFixtureResponse fixture : fixtures) {
            teamApiIds.add(fixture.homeTeamApiId());
            teamApiIds.add(fixture.awayTeamApiId());
            apiFootballIds.add(fixture.apiFootballId());
        }

        Map<Long, Team> teamMap = teamRepository.findAllByApiFootballIdIn(teamApiIds)
                .stream()
                .collect(Collectors.toMap(
                        Team::getApiFootballId,
                        Function.identity()
                ));

        Map<Long, Fixture> fixtureMap = fixtureRepository.findAllByApiFootballIdIn(apiFootballIds)
                .stream()
                .collect(Collectors.toMap(
                        Fixture::getApiFootballId,
                        Function.identity()
                ));


        for (ApiFootballFixtureResponse fixture : fixtures) {

            Long apiFootballId = fixture.apiFootballId();
            Long homeTeamApiId = fixture.homeTeamApiId();
            Long awayTeamApiId = fixture.awayTeamApiId();

            Team homeTeam = teamMap.get(homeTeamApiId);
            Team awayTeam = teamMap.get(awayTeamApiId);

            if ( homeTeam == null || awayTeam == null ) {
                throw new NotFoundTeamException();
            }
            LeagueCode leagueCode = LeagueCode.fromApiFootballLeagueId(fixture.leagueApiId());
            Integer season = fixture.season();
            String round = fixture.round();
            Instant kickoffAt = fixture.kickoffAt();
            FixtureStatus fixtureStatus = FixtureStatus.fromApiCode(fixture.status());
            Integer elapsed = fixture.elapsed();
            Integer extra = fixture.extra();
            Integer homeScore = fixture.homeScore();
            Integer awayScore = fixture.awayScore();
            String venueName = fixture.venueName();

            Fixture existFixture = fixtureMap.get(apiFootballId);

            if (existFixture == null) {
                Fixture savedFixture = fixtureRepository.save(Fixture.createFixture(
                        apiFootballId,
                        homeTeam,
                        awayTeam,
                        leagueCode,
                        season,
                        round,
                        kickoffAt,
                        fixtureStatus,
                        elapsed,
                        extra,
                        homeScore,
                        awayScore,
                        venueName
                ));

                fixtureMap.put(savedFixture.getApiFootballId(), savedFixture);

                log.info("경기생성 fixtureId={}", savedFixture.getId());
                continue;
            }

            log.info("경기 업데이트 fixtureId={}", existFixture.getId());
            existFixture.updateFixture(
                    kickoffAt,
                    fixtureStatus,
                    elapsed,
                    extra,
                    homeScore,
                    awayScore,
                    venueName
            );

        }
    }

}
