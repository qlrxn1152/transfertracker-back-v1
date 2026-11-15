package com.dhoon.transfertracker.internal.player.translation.service.impl;

import com.dhoon.transfertracker.external.openai.dto.request.PlayerNameTranslationTarget;
import com.dhoon.transfertracker.external.openai.dto.response.PlayerNameTranslationBatchResult;
import com.dhoon.transfertracker.external.openai.dto.response.PlayerNameTranslationResult;
import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.mockito.BDDMockito.*;
import static org.assertj.core.api.Assertions.*;


@ExtendWith(MockitoExtension.class)
public class PlayerNameTranslationTxServiceTest {

    @Mock
    private PlayerRepository playerRepository;

    @InjectMocks
    private PlayerNameTranslationTxService txService;

    @Test
    @DisplayName(value = "번역 대상 선수를 조회하고, 선수정보를 DTO 로 반환한다.")
    void findTranslationTargets() throws Exception {
        // given
        Player bruno = Player.of("Bruno", 100L);
        Player ronaldo = Player.of("Ronaldo", 101L);
        Player casemiro = Player.of("Casemiro", 102L);

        ReflectionTestUtils.setField(bruno, "id", 1L);
        ReflectionTestUtils.setField(ronaldo, "id", 2L);
        ReflectionTestUtils.setField(casemiro, "id", 3L);

        // 요청한 번역대상을 3명이라고 가정.
        int limit = 3;
        given(playerRepository.findPlayerNameTranslationTargets(PageRequest.of(0, limit)))
                .willReturn(List.of(bruno, ronaldo, casemiro));


        // when
        List<PlayerNameTranslationTarget> response = txService.findTranslationTargets(limit);

        // then
        assertThat(response).hasSize(3);
        assertThat(response).extracting(PlayerNameTranslationTarget::getPlayerName)
                .containsExactly("Bruno", "Ronaldo", "Casemiro");
        assertThat(response).extracting(PlayerNameTranslationTarget::getPlayerId)
                .containsExactly(1L, 2L, 3L);

        verify(playerRepository).findPlayerNameTranslationTargets(PageRequest.of(0, limit));
    }

    @Test
    @DisplayName(value = "번역 결과를 한국어 이름이 없는 Player에게 반영한다.")
    void applyTranslations() throws Exception {
        // given
        Player bruno = Player.of("Bruno", 100L);
        Player ronaldo = Player.of("Ronaldo", 101L);

        ReflectionTestUtils.setField(bruno, "id", 1L);
        ReflectionTestUtils.setField(ronaldo, "id", 2L);

        Set<Long> playerIdsSet = Set.of(1L, 2L);

        given(playerRepository.findAllById(playerIdsSet))
                .willReturn(List.of(bruno, ronaldo));

        PlayerNameTranslationBatchResult result = PlayerNameTranslationBatchResult.of(List.of(
                PlayerNameTranslationResult.of(1L, "브루노"),
                PlayerNameTranslationResult.of(2L, "호날두")
        ));

        // when
        txService.applyTranslations(result);

        // then
        assertThat(bruno.getPlayerNameKo()).isEqualTo("브루노");
        assertThat(ronaldo.getPlayerNameKo()).isEqualTo("호날두");
    }

    @Test
    @DisplayName(value = "한국어 이름이 이미 존재하는경우, 번역결과로 덮어씌어지지 않는다.")
    void applyTranslations_fail_alreadyAssigned_playerNameKo() throws Exception {
        // given
        Player bruno = Player.of("Bruno", 100L);
        bruno.assignKoPlayerName("브루노 페르난데스"); // 이미 할당.

        Player ronaldo = Player.of("Ronaldo", 101L);

        ReflectionTestUtils.setField(bruno, "id", 1L);
        ReflectionTestUtils.setField(ronaldo, "id", 2L);

        Set<Long> playerIdsSet = Set.of(1L, 2L);

        given(playerRepository.findAllById(playerIdsSet))
                .willReturn(List.of(bruno, ronaldo));

        PlayerNameTranslationBatchResult result = PlayerNameTranslationBatchResult.of(List.of(
                PlayerNameTranslationResult.of(1L, "브루노"),
                PlayerNameTranslationResult.of(2L, "호날두")
        ));

        // when
        txService.applyTranslations(result);

        // then
        assertThat(bruno.getPlayerNameKo()).isEqualTo("브루노 페르난데스");
        assertThat(ronaldo.getPlayerNameKo()).isEqualTo("호날두");
    }

    @Test
    @DisplayName(value = "DB에 번역결과의 선수들 ids 에 해당하는 id 값을 가지고있는 선수가 존재하지 않는경우, 전체 번역과정이 실패하고 예외가 발생한다.")
    void applyTranslations_fail_notFoundPlayer() throws Exception {
        // given
        Player bruno = Player.of("Bruno", 100L);
        ReflectionTestUtils.setField(bruno, "id", 1L);

        Set<Long> playerIdsSet = Set.of(1L, 2L); // 번역결과의 선수들 ids

        // DB 에 2L 이라는 id 를 가진 선수가 존재하지않음, 1L 인 선수만 존재.
        given(playerRepository.findAllById(playerIdsSet))
                .willReturn(List.of(bruno));

        PlayerNameTranslationBatchResult result = PlayerNameTranslationBatchResult.of(List.of(
                PlayerNameTranslationResult.of(1L, "브루노"),
                PlayerNameTranslationResult.of(2L, "호날두")
        ));

        // when && then
        assertThatThrownBy(() -> txService.applyTranslations(result))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("한글 이름을 반영할 Player를 찾을 수 없습니다.");

        assertThat(bruno.getPlayerNameKo()).isNull();
    }






}
