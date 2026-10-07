package com.dhoon.transfertracker.internal.transferpost.service.translation;

import com.dhoon.transfertracker.external.openai.client.OpenAiClient;
import com.dhoon.transfertracker.external.openai.dto.request.TranslationTarget;
import com.dhoon.transfertracker.external.openai.dto.response.TranslationBatchResult;
import com.dhoon.transfertracker.external.openai.dto.response.TranslationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransferPostTranslationServiceImpl {

    private final TransferPostTranslationTxService txService;
    private final OpenAiClient openAiClient;

    public TranslationBatchResult translate() {
        List<TranslationTarget> targets = txService.findPendingTargets();

        if ( targets.isEmpty() ) {
            return TranslationBatchResult.empty();
        }

        TranslationBatchResult result = openAiClient.translate(targets); // 외부 API 호출.

        Set<Long> requestedIds = targets.stream()
                .map(TranslationTarget::getPostId)
                .collect(Collectors.toSet());

        List<Long> responseIds = result.getPosts().stream()
                .map(TranslationResult::getPostId)
                .toList();

        Set<Long> responseIdSet = new HashSet<>(responseIds);

        if (responseIds.size() != responseIdSet.size()) {
            throw new IllegalStateException("번역 결과에 중복 postId가 존재합니다.");
        }

        if (!requestedIds.equals(responseIdSet)) {
            throw new IllegalStateException("번역 요청과 응답의 postId가 일치하지 않습니다.");
        }

        boolean hasInvalidContent = result.getPosts().stream()
                .anyMatch(post -> post.getTranslatedContent() == null || post.getTranslatedContent().isBlank());

        if (hasInvalidContent) {
            throw new IllegalStateException("번역 결과에 비어있는 translatedContent 가 존재합니다.");
        }


        txService.applyTranslations(result); // tx 필요.

        return result;
    }


}
