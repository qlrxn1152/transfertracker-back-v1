package com.dhoon.transfertracker.internal.player.translation.service.impl;

import com.dhoon.transfertracker.external.openai.client.OpenAiClient;
import com.dhoon.transfertracker.external.openai.dto.request.PlayerNameTranslationTarget;
import com.dhoon.transfertracker.external.openai.dto.response.PlayerNameTranslationBatchResult;
import com.dhoon.transfertracker.external.openai.dto.response.PlayerNameTranslationResult;
import com.dhoon.transfertracker.internal.player.translation.dto.response.PlayerNameTranslationResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerNameTranslationOrchestrationService {

    // 한번에 몇명의 번역대상을 가지고올지.
    private static final int FETCH_SIZE = 300;

    // OpenAI -> 한번에 몇명의 번역대상을 넘길지
    private static final int OPEN_AI_BATCH_SIZE = 50;

    private final PlayerNameTranslationTxService txService;
    private final OpenAiClient openAiClient;


    public PlayerNameTranslationResponseDto translate() {

        List<PlayerNameTranslationTarget> targets = txService.findTranslationTargets(FETCH_SIZE);

        // 번역대상이 존재하지 않는경우
        if (targets.isEmpty()) {
            return PlayerNameTranslationResponseDto.of(0, 0, 0);
        }

        int translatedCount = 0;
        int batchCount = 0;


        for (
                int start = 0;
                start < targets.size();
                start += OPEN_AI_BATCH_SIZE
        ) {

            int end = Math.min(start + OPEN_AI_BATCH_SIZE, targets.size());


            // 1st -> 0 ~ 50.
            List<PlayerNameTranslationTarget> batch = List.copyOf(
                            targets.subList(start, end)
                    );


            PlayerNameTranslationBatchResult result = openAiClient.translatePlayerNames(batch);
            PlayerNameTranslationBatchResult batchResult = getValidatedResult(batch, result);

            // Translation 작업 시작 -> DB 반영.
            txService.applyTranslations(batchResult);

            translatedCount += batchResult.getPlayers().size(); // 번역된 플레이어의 숫자

            batchCount++; // 몇번실행 ? BATCH_SIZE = 50 , FETCH_SIZE = 300 /// 정상적으로 모두 번역 -> BatchCount = 6.
        }


        return PlayerNameTranslationResponseDto.of(
                targets.size(),
                translatedCount,
                batchCount
        );

    }










    private PlayerNameTranslationBatchResult getValidatedResult(List<PlayerNameTranslationTarget> targets, PlayerNameTranslationBatchResult result) {

        // 번역 대상이 있어서 가지고왔는데 ( targets 검증을 통과했으므로 ) 번역 결과가 존재하지않으므로, 예외상황.
        if (result == null || result.getPlayers() == null) {
            throw new IllegalStateException("선수 이름 번역 결과가 존재하지 않습니다.");
        }

        // 중복된 플레이어 없이, 번역대상의 플레이어 IDs
        Set<Long> requestedIds = targets.stream()
                .map(PlayerNameTranslationTarget::getPlayerId)
                .collect(Collectors.toSet()); // set -> 중복제거

        List<PlayerNameTranslationResult> filteredPlayers = new ArrayList<>();

        result.getPlayers().forEach(player -> {

            if (player == null) {
                throw new IllegalArgumentException("번역 결과의 Player 가 null 값입니다.");
            }

            Long playerId = player.getPlayerId();
            String translatedPlayerName = player.getPlayerNameKo();

            if (playerId == null || playerId <= 0) {
                log.warn("Open API 가 넘겨준, 번역결과에 대해서 [ ID ] 값이 올바르지 않습니다");
                return;
            }

            if (translatedPlayerName == null || translatedPlayerName.isBlank() || translatedPlayerName.strip().equalsIgnoreCase("null")) {
                log.warn("Open API 가 넘겨준, 번역결과에 대해서 [ TranslatedPlayerName ] 값이 올바르지 않습니다 [ PlayerId = {} ]", playerId);
                return;
            }

            if (!requestedIds.contains(playerId)) {
                log.warn("선수 이름 번역 결과 제외, playerId = {}", playerId);
                return;
            }

            // 그래서, 누가 문제인건데 ?

            filteredPlayers.add(player); // 문제없는 player.
        });



        // 번역된 플레이어들의 IDs
        List<Long> responseIds = filteredPlayers.stream()
                        .map(PlayerNameTranslationResult::getPlayerId)
                        .toList();

        // 번역된 플레이어들의 IDs 중복제거
        Set<Long> responseIdSet = new HashSet<>(responseIds);

        // 번역 결과 과정에서, 중복된 아이디가 존재하는지 -> 즉, 중복된 플레이어가 존재하는지
        if (responseIds.size() != responseIdSet.size()) {
            throw new IllegalStateException("선수 이름 번역 결과에 중복 playerId가 존재합니다.");
        }

        // 요청한 플레이어의 IDs 랑, 결과로 응답받은 플레이어 IDs 일치하는지 -> 즉, 요청한 선수들을 모두 번역에 성공했는지
        if (!requestedIds.equals(responseIdSet)) {
            throw new IllegalStateException("선수 이름 번역 요청과 응답의 playerId가 일치하지 않습니다.");
        }

        return PlayerNameTranslationBatchResult.of(filteredPlayers);
    }


}
