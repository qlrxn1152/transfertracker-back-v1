package com.dhoon.transfertracker.internal.fixture.scheduler;
import com.dhoon.transfertracker.internal.fixture.service.FixturePlayerStatisticsSyncService;
import com.dhoon.transfertracker.internal.fixture.service.FixtureStatisticsSyncService;
import com.dhoon.transfertracker.internal.fixture.service.FixtureStatisticsTargetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Slf4j
@Component
@RequiredArgsConstructor
@Profile({"local", "prod"})
public class FixtureStatisticsScheduler {
    private final FixtureStatisticsTargetService targetService;
    private final FixtureStatisticsSyncService statisticsSyncService;
    private final FixturePlayerStatisticsSyncService playerSyncService;
    @Scheduled(fixedDelay = 600_000)
    public void sync() {
        for (Long id : targetService.getTargets(false)) { try { statisticsSyncService.syncFixtureStatistics(id); } catch (Exception e) { log.error("Team stats sync failed: {}", id, e); } }
        for (Long id : targetService.getTargets(true)) { try { playerSyncService.syncFixturePlayerStatistics(id); } catch (Exception e) { log.error("Player stats sync failed: {}", id, e); } }
    }
}
