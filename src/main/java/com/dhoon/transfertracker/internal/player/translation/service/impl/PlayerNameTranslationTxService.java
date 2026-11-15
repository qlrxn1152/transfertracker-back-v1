package com.dhoon.transfertracker.internal.player.translation.service.impl;

import com.dhoon.transfertracker.external.openai.dto.request.PlayerNameTranslationTarget;
import com.dhoon.transfertracker.external.openai.dto.response.PlayerNameTranslationBatchResult;
import com.dhoon.transfertracker.external.openai.dto.response.PlayerNameTranslationResult;
import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.teamplayer.repository.TeamPlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PlayerNameTranslationTxService {

    private final PlayerRepository playerRepository;

    @Transactional(readOnly = true)
    public List<PlayerNameTranslationTarget> findTranslationTargets(int limit) {
        return playerRepository.findPlayerNameTranslationTargets(
                        PageRequest.of(
                                0,
                                limit
                        )
                )
                .stream()
                .map(PlayerNameTranslationTarget::of)
                .toList();
    }


    // 다 성공 OR 실패 1명이라도있으면 -> 다 실패
    public void applyTranslations(PlayerNameTranslationBatchResult result) {

        List<Long> playerIds = result.getPlayers()
                        .stream()
                        .map(PlayerNameTranslationResult::getPlayerId)
                        .toList();


        // <플레이어Id, 번역된 한국어이름>
        Map<Long, String> translatedNames = result.getPlayers()
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        PlayerNameTranslationResult::getPlayerId,
                                        PlayerNameTranslationResult::getPlayerNameKo
                                )
                        );

        List<Player> players = playerRepository.findAllById(playerIds);


        // 특정 플레이어가 존재하지 않는경우
        if (players.size() != playerIds.size()) {
            throw new IllegalStateException("한글 이름을 반영할 Player를 찾을 수 없습니다.");
        }


        players.forEach(player -> {
            String playerNameKo = translatedNames.get(player.getId());


            // 한국어 이름이 존재하지 않는 플레이어에게 한국어 이름 할당
            if (player.getPlayerNameKo() == null || player.getPlayerNameKo().isBlank()) {
                player.assignKoPlayerName(playerNameKo);
            }

        });
    }
}
