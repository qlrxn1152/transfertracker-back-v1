package com.dhoon.transfertracker.internal.transferpost.translation.batch.reader;

import com.dhoon.transfertracker.external.openai.dto.request.TranslationTarget;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPost;
import com.dhoon.transfertracker.internal.transferpost.repository.TransferPostRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.data.RepositoryItemReader;
import org.springframework.batch.infrastructure.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
@StepScope
public class TransferPostTranslationItemReader implements ItemReader<RepositoryItemReader<TransferPost>> {

    private final TransferPostRepository transferPostRepository;

    @Override
    public @Nullable RepositoryItemReader<TransferPost> read() throws Exception {
        return new RepositoryItemReaderBuilder<TransferPost>()
                .name("beforeReader")
                .pageSize(50)
                .methodName("findLatestPendingTranslationPosts")
                .repository(transferPostRepository)
                .build();
    }

}
