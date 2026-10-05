package com.dhoon.transfertracker.internal.player.service.impl;

import com.dhoon.transfertracker.external.openai.dto.request.PlayerNameTranslationTarget;
import com.dhoon.transfertracker.external.openai.dto.response.PlayerNameTranslationBatchResult;
import com.dhoon.transfertracker.external.openai.dto.response.PlayerNameTranslationResult;
import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.teamplayer.repository.TeamPlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PlayerNameTranslationTxService {

    private final PlayerRepository playerRepository;
    private final TeamPlayerRepository teamPlayerRepository;


    @Transactional(readOnly = true)
    public List<PlayerNameTranslationTarget> findTargets(
            int limit
    ) {

        return teamPlayerRepository
                .findPlayerNameTranslationTargets(
                        PageRequest.of(
                                0,
                                limit
                        )
                )
                .stream()
                .map(teamPlayer ->
                        new PlayerNameTranslationTarget(
                                teamPlayer.getPlayer().getId(),
                                teamPlayer.getPlayer().getPlayerName(),
                                teamPlayer.getTeam().getTeamName()
                        )
                )
                .toList();
    }


    public void applyTranslations(
            PlayerNameTranslationBatchResult result
    ) {

        List<Long> playerIds =
                result.getPlayers()
                        .stream()
                        .map(
                                PlayerNameTranslationResult::getPlayerId
                        )
                        .toList();


        Map<Long, String> translatedNames =
                result.getPlayers()
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        PlayerNameTranslationResult::getPlayerId,
                                        PlayerNameTranslationResult::getPlayerNameKo
                                )
                        );


        List<Player> players =
                playerRepository
                        .findAllById(
                                playerIds
                        );


        /*
         * 요청 이후 Player가 삭제되는 등의 비정상 상황.
         *
         * 일부 Player만 변경되는 것을 방지하기 위해
         * 전체 ID가 존재하는지 먼저 확인한다.
         */
        if (players.size() != playerIds.size()) {

            throw new IllegalStateException(
                    "한글 이름을 반영할 Player를 찾을 수 없습니다."
            );
        }


        players.forEach(player -> {

            String playerNameKo =
                    translatedNames.get(
                            player.getId()
                    );


            /*
             * 번역 대상 조회 이후 사람이 직접 값을 수정한 경우에는
             * AI 결과로 덮어쓰지 않는다.
             */
            if (player.getPlayerNameKo() == null) {

                player.assignKoPlayerName(
                        playerNameKo
                );
            }
        });
    }
}