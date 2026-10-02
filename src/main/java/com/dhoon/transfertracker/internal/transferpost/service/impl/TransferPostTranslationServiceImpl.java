package com.dhoon.transfertracker.internal.transferpost.service.impl;

import com.dhoon.transfertracker.external.openai.client.OpenAiClient;
import com.dhoon.transfertracker.external.openai.dto.request.TranslationTarget;
import com.dhoon.transfertracker.external.openai.dto.response.TranslationBatchResult;
import com.dhoon.transfertracker.internal.transferpost.repository.TransferPostRepository;
import com.dhoon.transfertracker.internal.transferpost.service.TransferPostTranslationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransferPostTranslationServiceImpl implements TransferPostTranslationService {

    private final TransferPostTranslationTxService txService;
    private final OpenAiClient openAiClient;

    public TranslationBatchResult translate() {
        List<TranslationTarget> targets = txService.findPendingTargets();

        if ( targets.isEmpty() ) {
            return TranslationBatchResult.empty();
        }

        TranslationBatchResult result = openAiClient.translate(targets);

        txService.applyTranslations(result);

        return result;
    }


}
