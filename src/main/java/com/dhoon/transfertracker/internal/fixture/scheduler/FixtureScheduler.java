package com.dhoon.transfertracker.internal.fixture.scheduler;

import com.dhoon.transfertracker.internal.fixture.service.FixtureSyncService;
import com.dhoon.transfertracker.internal.fixture.service.FixtureSyncTxService;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile({"local", "prod"})
public class FixtureScheduler {

    private final FixtureSyncService fixtureSyncService;
    private final FixtureSyncTxService fixtureSyncTxService;

    @Scheduled(fixedDelay = 30_000)
    public void syncLiveFixtures() {
        boolean hasSyncFixture =
                fixtureSyncTxService.existsFixturesToSyncLive();

        if (!hasSyncFixture) {
            log.debug("실시간 경기 동기화 대상 없음");
            return;
        }

        log.info("실시간 경기 동기화 시작");

        fixtureSyncService.syncLiveFixtures();

        log.info("실시간 경기 동기화 완료");
    }

    @Scheduled(fixedDelay = 300_000)
    public void syncFixtureCompletion() {
        log.info("경기 종료 동기화 시작");

        fixtureSyncService.syncFixtureCompletion();

        log.info("경기 종료 동기화 완료");
    }

    @Scheduled(cron = "0 0 10 * * *", zone = "Asia/Seoul")
    public void syncSeasonFixtures() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

        for (LeagueCode leagueCode : LeagueCode.values()) {
            int season = leagueCode.resolveSeason(today);

            log.info("시즌 경기 동기화 league={}, season={}",
                    leagueCode, season);

            fixtureSyncService.syncLeagueFixtures(leagueCode, season);
        }
    }
}
