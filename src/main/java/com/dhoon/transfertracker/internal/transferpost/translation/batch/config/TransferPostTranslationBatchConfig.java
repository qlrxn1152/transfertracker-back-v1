package com.dhoon.transfertracker.internal.transferpost.translation.batch.config;

import com.dhoon.transfertracker.external.openai.dto.request.TranslationTarget;
import com.dhoon.transfertracker.external.openai.dto.response.TranslationBatchResult;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPost;
import com.dhoon.transfertracker.internal.transferpost.translation.batch.processor.TransferPostTranslationItemProcessor;
import com.dhoon.transfertracker.internal.transferpost.translation.batch.reader.TransferPostTranslationItemReader;
import com.dhoon.transfertracker.internal.transferpost.translation.batch.writer.TransferPostTranslationItemWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.data.RepositoryItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class TransferPostTranslationBatchConfig {


}
