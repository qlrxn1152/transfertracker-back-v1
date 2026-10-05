package com.dhoon.transfertracker.internal.player.service.impl;

import com.dhoon.transfertracker.external.openai.client.OpenAiClient;
import com.dhoon.transfertracker.external.openai.dto.request.PlayerNameTranslationTarget;
import com.dhoon.transfertracker.external.openai.dto.response.PlayerNameTranslationBatchResult;
import com.dhoon.transfertracker.external.openai.dto.response.PlayerNameTranslationResult;
import com.dhoon.transfertracker.internal.player.dto.response.PlayerNameTranslationResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlayerNameTranslationService {

    /*
     * DB에서는 한 번에 최대 300명의
     * 번역 대상 선수를 가져온다.
     */
    private static final int FETCH_SIZE = 300;


    /*
     * OpenAI에는 300명을 한 번에 보내지 않고
     * 50명씩 나누어서 요청한다.
     *
     * 특정 Batch가 실패했을 때의 영향 범위를 줄이고,
     * JSON 응답 검증도 쉽게 하기 위함.
     */
    private static final int OPEN_AI_BATCH_SIZE = 50;


    private final PlayerNameTranslationTxService txService;
    private final OpenAiClient openAiClient;


    public PlayerNameTranslationResponseDto translate() {

        List<PlayerNameTranslationTarget> targets =
                txService.findTargets(
                        FETCH_SIZE
                );


        if (targets.isEmpty()) {

            return PlayerNameTranslationResponseDto.of(
                    0,
                    0,
                    0
            );
        }


        int translatedCount = 0;
        int batchCount = 0;


        for (
                int start = 0;
                start < targets.size();
                start += OPEN_AI_BATCH_SIZE
        ) {

            int end =
                    Math.min(
                            start + OPEN_AI_BATCH_SIZE,
                            targets.size()
                    );


            /*
             * subList View 자체를 외부 Client에 넘기지 않고
             * 독립 List로 만들어 전달한다.
             */
            List<PlayerNameTranslationTarget> batch =
                    List.copyOf(
                            targets.subList(
                                    start,
                                    end
                            )
                    );


            PlayerNameTranslationBatchResult result =
                    openAiClient
                            .translatePlayerNames(
                                    batch
                            );


            validateResult(
                    batch,
                    result
            );


            /*
             * OpenAI HTTP 호출이 모두 끝난 뒤
             * 여기서 짧은 DB Transaction이 시작된다.
             */
            txService.applyTranslations(
                    result
            );


            translatedCount +=
                    result.getPlayers().size();

            batchCount++;
        }


        return PlayerNameTranslationResponseDto.of(
                targets.size(),
                translatedCount,
                batchCount
        );
    }


    private void validateResult(
            List<PlayerNameTranslationTarget> targets,
            PlayerNameTranslationBatchResult result
    ) {

        if (
                result == null
                || result.getPlayers() == null
        ) {

            throw new IllegalStateException(
                    "선수 이름 번역 결과가 존재하지 않습니다."
            );
        }


        Set<Long> requestedIds =
                targets.stream()
                        .map(
                                PlayerNameTranslationTarget::getPlayerId
                        )
                        .collect(
                                Collectors.toSet()
                        );


        List<Long> responseIds =
                result.getPlayers()
                        .stream()
                        .map(
                                PlayerNameTranslationResult::getPlayerId
                        )
                        .toList();


        Set<Long> responseIdSet =
                new HashSet<>(
                        responseIds
                );


        if (
                responseIds.size()
                != responseIdSet.size()
        ) {

            throw new IllegalStateException(
                    "선수 이름 번역 결과에 중복 playerId가 존재합니다."
            );
        }


        if (
                !requestedIds.equals(
                        responseIdSet
                )
        ) {

            throw new IllegalStateException(
                    "선수 이름 번역 요청과 응답의 playerId가 일치하지 않습니다."
            );
        }


        boolean hasInvalidName =
                result.getPlayers()
                        .stream()
                        .anyMatch(
                                player ->
                                        player.getPlayerNameKo() == null
                                        || player.getPlayerNameKo().isBlank()
                        );


        if (hasInvalidName) {

            throw new IllegalStateException(
                    "선수 이름 번역 결과에 비어있는 playerNameKo가 존재합니다."
            );
        }
    }
}