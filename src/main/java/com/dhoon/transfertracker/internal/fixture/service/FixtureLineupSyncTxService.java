package com.dhoon.transfertracker.internal.fixture.service;

import com.dhoon.transfertracker.internal.fixture.domain.*;
import com.dhoon.transfertracker.internal.fixture.dto.record.ApiFootballFixtureLineupPlayerResponse;
import com.dhoon.transfertracker.internal.fixture.dto.record.ApiFootballFixtureLineupResponse;
import com.dhoon.transfertracker.internal.fixture.repository.FixtureRepository;
import com.dhoon.transfertracker.internal.fixture.repository.FixtureLineupRepository;
import com.dhoon.transfertracker.internal.fixture.repository.FixtureLineupPlayerRepository;
import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.team.domain.Team;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class FixtureLineupSyncTxService {

    private final FixtureRepository fixtureRepository;
    private final FixtureLineupRepository fixtureLineupRepository;
    private final FixtureLineupPlayerRepository fixtureLineupPlayerRepository;
    private final PlayerRepository playerRepository;

    public void syncFixtureLineups(
            Long fixtureApiId,
            List<ApiFootballFixtureLineupResponse> lineups
    ) {

        Fixture fixture = fixtureRepository
                .findByApiFootballId(fixtureApiId)
                .orElse(null);

        if (fixture == null) {
            log.debug(
                    "라인업 동기화 대상 경기 없음. fixtureApiId={}",
                    fixtureApiId
            );
            return;
        }

        if (!isValidLineups(fixture, lineups)) {
            log.debug(
                    "불완전한 경기 라인업. fixtureApiId={}",
                    fixtureApiId
            );
            return;
        }

        for (ApiFootballFixtureLineupResponse response : lineups) {

            Team team = resolveTeam(
                    fixture,
                    response.teamApiId()
            );

            FixtureLineup fixtureLineup = fixtureLineupRepository
                    .findByFixtureIdAndTeamId(
                            fixture.getId(),
                            team.getId()
                    )
                    .orElseGet(() -> fixtureLineupRepository.save(
                            FixtureLineup.createFixtureLineup(
                                    fixture,
                                    team,
                                    response.formation()
                            )
                    ));

            fixtureLineup.updateFormation(response.formation());

            List<FixtureLineupPlayer> lineupPlayers =
                    new ArrayList<>();

            for (ApiFootballFixtureLineupPlayerResponse playerResponse
                    : response.players()) {

                Player player = resolvePlayer(
                        playerResponse.playerApiId(),
                        playerResponse.playerName()
                );

                FixtureLineupPlayer lineupPlayer =
                        FixtureLineupPlayer.createFixtureLineupPlayer(
                                fixtureLineup,
                                player,
                                playerResponse.number(),
                                playerResponse.position(),
                                playerResponse.grid(),
                                playerResponse.status()
                        );

                lineupPlayers.add(lineupPlayer);
            }

            fixtureLineupPlayerRepository.deleteAllByFixtureLineupId(
                    fixtureLineup.getId()
            );

            fixtureLineupPlayerRepository.saveAll(lineupPlayers);
        }

        fixture.completeLineupSync();

        log.info(
                "경기 라인업 동기화 완료. fixtureId={}, lineupCount={}",
                fixture.getId(),
                lineups.size()
        );
    }

    @Transactional(readOnly = true)
    public List<Long> getFixturesToSyncLineups() {

        Instant now = Instant.now();

        Instant from = now.minus(3, ChronoUnit.HOURS);
        Instant to = now.plus(1, ChronoUnit.HOURS);

        List<FixtureStatus> statuses = List.of(
                FixtureStatus.NOT_STARTED,
                FixtureStatus.FIRST_HALF,
                FixtureStatus.HALF_TIME,
                FixtureStatus.SECOND_HALF,
                FixtureStatus.EXTRA_TIME,
                FixtureStatus.BREAK_TIME,
                FixtureStatus.PENALTY_IN_PROGRESS,
                FixtureStatus.LIVE,
                FixtureStatus.FINISHED,
                FixtureStatus.AFTER_EXTRA_TIME,
                FixtureStatus.AFTER_PENALTY
        );

        return fixtureRepository.findFixturesToSyncLineups(
                        from,
                        to,
                        statuses,
                        PageRequest.of(0, 20)
                )
                .stream()
                .map(Fixture::getApiFootballId)
                .toList();
    }


    private Team resolveTeam(Fixture fixture, Long teamApiId) {

        if (Objects.equals(
                fixture.getHomeTeam().getApiFootballId(),
                teamApiId
        )) {
            return fixture.getHomeTeam();
        }

        if (Objects.equals(
                fixture.getAwayTeam().getApiFootballId(),
                teamApiId
        )) {
            return fixture.getAwayTeam();
        }

        throw new IllegalArgumentException(
                "해당 경기에 포함되지 않은 팀입니다. teamApiId="
                        + teamApiId
        );
    }

    private Player resolvePlayer(
            Long playerApiId,
            String playerName
    ) {
        return playerRepository
                .findByApiFootballId(playerApiId)
                .orElseGet(() -> playerRepository.save(
                        Player.of(playerName, playerApiId)
                ));
    }

    private boolean isValidLineups(
            Fixture fixture,
            List<ApiFootballFixtureLineupResponse> lineups
    ) {

        if (lineups == null || lineups.size() != 2) {
            return false;
        }

        boolean hasHomeTeam = false;
        boolean hasAwayTeam = false;

        for (ApiFootballFixtureLineupResponse lineup : lineups) {

            if (Objects.equals(
                    fixture.getHomeTeam().getApiFootballId(),
                    lineup.teamApiId()
            )) {
                hasHomeTeam = true;
            }

            if (Objects.equals(
                    fixture.getAwayTeam().getApiFootballId(),
                    lineup.teamApiId()
            )) {
                hasAwayTeam = true;
            }

            long starterCount = lineup.players().stream()
                    .filter(player ->
                            player.status() ==
                                    FixtureLineupPlayerStatus.STARTER
                    )
                    .count();

            if (starterCount != 11) {
                return false;
            }

            long distinctPlayerCount = lineup.players().stream()
                    .map(ApiFootballFixtureLineupPlayerResponse::playerApiId)
                    .distinct()
                    .count();

            if (distinctPlayerCount != lineup.players().size()) {
                return false;
            }
        }

        return hasHomeTeam && hasAwayTeam;
    }


}