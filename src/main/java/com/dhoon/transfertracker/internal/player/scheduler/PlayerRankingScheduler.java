package com.dhoon.transfertracker.internal.player.scheduler;
import com.dhoon.transfertracker.internal.player.service.PlayerRankingSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Component
@RequiredArgsConstructor
@Profile({"local", "prod"})
public class PlayerRankingScheduler {
    private final PlayerRankingSyncService syncService;
    @Scheduled(fixedDelay = 3_600_000)
    public void sync() { syncService.syncAll(); }
}
