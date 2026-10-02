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

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class TransferPostTranslationTxService {

    private final TransferPostRepository transferPostRepository;

    @Transactional(readOnly = true)
    public List<TranslationTarget> findPendingTargets() {
        return transferPostRepository.findTop10ByTranslateStatusOrderByContentCreatedAtDescIdDesc(TranslateStatus.PENDING)
                .stream()
                .map(transferPost -> new TranslationTarget(transferPost.getId(), transferPost.getContent()))
                .toList();
    }

    public void applyTranslations(TranslationBatchResult result) {

        List<TranslationResult> a = result.getPosts();

        for (TranslationResult translationResult : a) {
            Long successPostId = translationResult.getPostId();

            transferPostRepository.findById(successPostId)
                    .ifPresent(transferPost ->
                        transferPost.translate(translationResult.getTranslatedContent())
                    );
        }
    }




}
