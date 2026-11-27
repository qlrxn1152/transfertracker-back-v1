package com.dhoon.transfertracker.internal.standing.scheduler;

import com.dhoon.transfertracker.internal.standing.service.LeagueStandingSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile({"local", "prod"})
public class LeagueStandingScheduler {

    private final LeagueStandingSyncService leagueStandingSyncService;

    @Scheduled(fixedDelay = 3_600_000)
    public void syncLeagueStandings() {

        log.info("전체 리그 순위 동기화 시작");

        leagueStandingSyncService.syncAllLeagueStandings();

        log.info("전체 리그 순위 동기화 작업 종료");
    }
}