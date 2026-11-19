package com.dhoon.transfertracker.internal.transferpost.sync.batch.scheduler;

import com.dhoon.transfertracker.internal.post.domain.PostJournalist;
import com.dhoon.transfertracker.internal.post.service.PostSaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransferPostSyncScheduler {

    private final PostSaveService postSaveService;

    @Scheduled(cron = "0 0 9,21 * * *", zone = "Asia/Seoul")
    public void syncPosts() {
        postSaveService.syncPosts(PostJournalist.FABRIZIO_ROMANO);
    }
}
