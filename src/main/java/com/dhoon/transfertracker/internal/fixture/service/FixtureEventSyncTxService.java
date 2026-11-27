package com.dhoon.transfertracker.internal.fixture.service;

import com.dhoon.transfertracker.internal.fixture.domain.Fixture;
import com.dhoon.transfertracker.internal.fixture.domain.FixtureEvent;
import com.dhoon.transfertracker.internal.fixture.domain.FixtureEventType;
import com.dhoon.transfertracker.internal.fixture.domain.FixtureStatus;
import com.dhoon.transfertracker.internal.fixture.dto.record.ApiFootballFixtureEventResponse;
import com.dhoon.transfertracker.internal.fixture.exception.NotFoundFixtureException;
import com.dhoon.transfertracker.internal.fixture.repository.FixtureEventRepository;
import com.dhoon.transfertracker.internal.fixture.repository.FixtureRepository;
import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.player.sync.dto.PlayerSyncData;
import com.dhoon.transfertracker.internal.player.sync.service.impl.PlayerSyncTxService;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.team.sync.service.impl.TeamSyncTxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class FixtureEventSyncTxService {

    private final FixtureRepository fixtureRepository;
    private final FixtureEventRepository fixtureEventRepository;
    private final PlayerRepository playerRepository;

    public void syncFixtureEvents(Long fixtureApiId, List<ApiFootballFixtureEventResponse> events) {
        Fixture fixture = fixtureRepository.findByApiFootballId(fixtureApiId).orElseThrow(NotFoundFixtureException::new);

        List<FixtureEvent> fixtureEvents = new ArrayList<>();

        for (ApiFootballFixtureEventResponse event : events) {

            Team team = resolveTeam(fixture, event.teamApiId());

            Player player = resolvePlayer(
                    event.playerApiId(),
                    event.playerName()
            );

            Player relatedPlayer = resolvePlayer(
                    event.relatedPlayerApiId(),
                    event.relatedPlayerName()
            );

            fixtureEvents.add(FixtureEvent.createFixtureEvent(
                    fixture,
                    team,
                    player,
                    relatedPlayer,
                    event.elapsed(),
                    event.extra(),
                    FixtureEventType.fromApiCode(event.type()),
                    event.detail(),
                    event.comments()
            ));
        }

        fixtureEventRepository.deleteAllByFixtureId(fixture.getId());

        fixtureEventRepository.saveAll(fixtureEvents);

        if (fixture.getStatus() == FixtureStatus.FINISHED
                || fixture.getStatus() == FixtureStatus.AFTER_EXTRA_TIME
                || fixture.getStatus() == FixtureStatus.AFTER_PENALTY) {

            fixture.completeEventSync();
        }

        log.info("경기 이벤트 동기화 완료. fixtureId={}, eventCount={}",
                fixture.getId(), fixtureEvents.size());
    }

    private Team resolveTeam(Fixture fixture, Long teamApiId) {

        if (Objects.equals(fixture.getHomeTeam().getApiFootballId(), teamApiId)) {
            return fixture.getHomeTeam();
        }

        if (Objects.equals(fixture.getAwayTeam().getApiFootballId(), teamApiId)) {
            return fixture.getAwayTeam();
        }

        throw new IllegalArgumentException("해당 경기에 참여하지 않은 팀입니다. teamApiId=" + teamApiId);
    }

    private Player resolvePlayer(Long playerApiId, String playerName) {

        if (playerApiId == null) {
            return null;
        }

        return playerRepository.findByApiFootballId(playerApiId)
                .orElseGet(() -> playerRepository.save(
                        Player.of(playerName, playerApiId)
                ));
    }


    @Transactional(readOnly = true)
    public List<Long> getFixturesToSyncEvents() {

        List<FixtureStatus> statuses = List.of(
                FixtureStatus.FINISHED,
                FixtureStatus.AFTER_EXTRA_TIME,
                FixtureStatus.AFTER_PENALTY
        );

        return fixtureRepository.findFixturesToSyncEvents(statuses, PageRequest.of(0, 20))
                .stream()
                .map(Fixture::getApiFootballId)
                .toList();
    }
}