package com.dhoon.transfertracker.internal.transferpost.service.impl;

import com.dhoon.transfertracker.external.openai.dto.request.TranslationTarget;
import com.dhoon.transfertracker.external.openai.dto.response.TranslationBatchResult;
import com.dhoon.transfertracker.external.openai.dto.response.TranslationResult;
import com.dhoon.transfertracker.internal.transferpost.domain.TranslateStatus;
import com.dhoon.transfertracker.internal.transferpost.repository.TransferPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class TransferPostTranslationTxService {

    private final TransferPostRepository transferPostRepository;

    @Transactional(readOnly = true)
    public List<TranslationTarget> findPendingTargets() {
        return transferPostRepository.findTop20ByTranslateStatusOrderByContentCreatedAtDescIdDesc(TranslateStatus.PENDING)
                .stream()
                .map(transferPost -> new TranslationTarget(transferPost.getId(), transferPost.getContent()))
                .toList();
    }

    public void applyTranslations(TranslationBatchResult result) {

        List<Long> postIds = result.getPosts().stream()
                .map(TranslationResult::getPostId)
                .toList(); // OpenAI 가 넘겨준 postId

        Map<Long, String> translatedContentByPostId = result.getPosts().stream()
                .collect(Collectors.toMap(
                        TranslationResult::getPostId,
                        TranslationResult::getTranslatedContent
                )); // OpenAI 가 넘겨준 < PostId, 번역된 content >

        transferPostRepository.findAllById(postIds)
                .forEach(transferPost -> {
                    String translatedContent =
                            translatedContentByPostId.get(transferPost.getId());

                    transferPost.translate(translatedContent);
                });
    }




}
