package com.dhoon.transfertracker.internal.player.scheduler;
import com.dhoon.transfertracker.internal.player.service.PlayerSeasonStatisticsSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Component
@RequiredArgsConstructor
@Profile({"local", "prod"})
public class PlayerSeasonStatisticsScheduler {
    private final PlayerSeasonStatisticsSyncService syncService;
    @Scheduled(cron = "0 0 11 * * *", zone = "Asia/Seoul")
    public void sync() { syncService.syncAllLeagues(); }
}
