package com.dhoon.transfertracker.internal.transferpost.sync.batch.scheduler;

import com.dhoon.transfertracker.internal.transferpost.sync.batch.config.TransferPostSyncBatchConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransferPostSyncScheduler {

    private final JobOperator jobOperator;
    private final Job transferPostSyncJob;

    @Scheduled(cron = "0 0 9,21 * * *", zone = "Asia/Seoul")
    public void syncTransferPosts() throws Exception{
        JobParameters parameters = new JobParametersBuilder()
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();

        jobOperator.start(transferPostSyncJob, parameters);
    }
}
