package com.dhoon.transfertracker.internal.player.sync.service.impl;


import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.player.sync.dto.PlayerSyncData;
import com.dhoon.transfertracker.internal.player.sync.dto.response.PlayerSaveResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.BDDMockito.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class PlayerSyncTxServiceTest {

    @Mock
    private PlayerRepository playerRepository;

    @InjectMocks
    private PlayerSyncTxService txService;

    @Test
    @DisplayName("기존 선수가 존재하면 신규 저장하지 않고 반환한다")
    void getExistingPlayer() {
        // given
        PlayerSyncData syncData = new PlayerSyncData(100L, "testPlayer");

        Player player = Player.of("testPlayer", 100L);

        given(playerRepository.findByApiFootballId(100L))
                .willReturn(Optional.of(player));

        // when
        PlayerSaveResponseDto response = txService.getOrCreatePlayerResponse(syncData);

        // then
        assertThat(response.getPlayerAPIId()).isEqualTo(100L);
        assertThat(response.getPlayerName()).isEqualTo("testPlayer");

        verify(playerRepository, times(1)).findByApiFootballId(100L);
        verify(playerRepository, never()).save(any(Player.class));
    }

    @Test
    @DisplayName("선수가 존재하지 않는다면, 신규 저장하고 반환한다")
    void get_notExistingPlayer() {
        // given
        PlayerSyncData syncData = new PlayerSyncData(100L, "testPlayer");

        given(playerRepository.findByApiFootballId(100L))
                .willReturn(Optional.empty());

        given(playerRepository.save(any(Player.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        PlayerSaveResponseDto response = txService.getOrCreatePlayerResponse(syncData);

        // then
        ArgumentCaptor<Player> captor = ArgumentCaptor.forClass(Player.class);

        assertThat(response.getPlayerAPIId()).isEqualTo(100L);
        assertThat(response.getPlayerName()).isEqualTo("testPlayer");

        verify(playerRepository, times(1)).findByApiFootballId(100L);
        verify(playerRepository, times(1)).save(captor.capture());

        Player savedPlayer = captor.getValue();

        assertThat(savedPlayer.getApiFootballId()).isEqualTo(100L);
        assertThat(savedPlayer.getPlayerName()).isEqualTo("testPlayer");
    }



}
