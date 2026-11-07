package com.dhoon.transfertracker.internal.transferpost.sync.batch.config;

import com.dhoon.transfertracker.internal.transferpost.domain.TransferPost;
import com.dhoon.transfertracker.internal.transferpost.sync.batch.dto.TransferPostRawItem;
import com.dhoon.transfertracker.internal.transferpost.sync.batch.processor.TransferPostItemProcessor;
import com.dhoon.transfertracker.internal.transferpost.sync.batch.reader.TransferPostItemReader;
import com.dhoon.transfertracker.internal.transferpost.sync.batch.writer.TransferPostItemWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class TransferPostSyncBatchConfig {


    @Bean
    public Step transferPostSyncStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            TransferPostItemReader reader,
            TransferPostItemProcessor processor,
            TransferPostItemWriter writer
    ) {
        return new StepBuilder("transferPostSyncStep", jobRepository)
                .<TransferPostRawItem, TransferPost>chunk(30)
                .transactionManager(transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean
    public Job transferPostSyncJob(
            JobRepository jobRepository,
            Step transferPostSyncStep
    ) {
        return new JobBuilder("transferPostSyncJob", jobRepository)
                .start(transferPostSyncStep)
                .build();
    }

}
