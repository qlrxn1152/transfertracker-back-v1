package com.dhoon.transfertracker.internal.transferpost.translation.batch.processor;

import com.dhoon.transfertracker.external.openai.client.OpenAiClient;
import com.dhoon.transfertracker.external.openai.dto.request.TranslationTarget;
import com.dhoon.transfertracker.external.openai.dto.response.TranslationBatchResult;
import com.dhoon.transfertracker.external.openai.dto.response.TranslationResult;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPost;
import com.dhoon.transfertracker.internal.transferpost.exception.NotFoundTransferPostException;
import com.dhoon.transfertracker.internal.transferpost.repository.TransferPostRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Configuration
@RequiredArgsConstructor
public class TransferPostTranslationItemProcessor implements ItemProcessor<List<TranslationTarget>, TranslationBatchResult> {

    private final OpenAiClient openAiClient;
    private final TransferPostRepository transferPostRepository;


    @Override
    public @Nullable TranslationBatchResult process(List<TranslationTarget> item) throws Exception {

        TranslationBatchResult result = openAiClient.translate(item);

        Set<Long> resultIds = result.getPosts().stream()
                .map(TranslationResult::getPostId)
                .collect(Collectors.toSet());

        // 번역이 안된 게시물이 존재
        if ( item.size() != resultIds.size() ) {
            throw new IllegalStateException("번역이 안된 게시물이 존재합니다.");
        }

        Set<Long> requestIds = item.stream()
                .map(TranslationTarget::getPostId)
                .collect(Collectors.toSet());

        // 번역이 됐지만, Post ID 에 왜곡이 생긴경우.
        if (!requestIds.containsAll(resultIds)) {
            throw new IllegalStateException("번역결과가 왜곡된 포스트가 존재합니다.");
        }


        // 번역된 결과에 문제가 있는경우
        boolean isInvalidContent = result.getPosts().stream()
                .anyMatch(post -> post.getTranslatedContent() == null || post.getTranslatedContent().isBlank());

        if (isInvalidContent) {
            throw new IllegalStateException("번역된 결과에 문제가 있습니다.");
        }

        return result;
    }


}
