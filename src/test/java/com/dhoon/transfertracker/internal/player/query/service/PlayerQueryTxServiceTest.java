package com.dhoon.transfertracker.internal.player.query.service;

import com.dhoon.transfertracker.internal.player.query.service.impl.PlayerQueryTxService;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.teamplayer.repository.TeamPlayerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PlayerQueryTxServiceTest {

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private TeamPlayerRepository teamPlayerRepository;
    
    @InjectMocks
    private PlayerQueryTxService playerQueryTxService;

    @Test
    @DisplayName(value = "")
    void getPlayer_success() throws Exception {
        // given


        // when

        // then

    }
}
