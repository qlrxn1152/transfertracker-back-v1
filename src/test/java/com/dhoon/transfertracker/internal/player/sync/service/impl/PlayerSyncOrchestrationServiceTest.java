package com.dhoon.transfertracker.internal.player.sync.service.impl;

import com.dhoon.transfertracker.external.football.client.ApiFootballHttpClient;
import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.player.sync.dto.PlayerSyncData;
import com.dhoon.transfertracker.internal.player.sync.dto.response.PlayerSaveResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;

import static org.mockito.BDDMockito.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class PlayerSyncOrchestrationServiceTest {

    @Mock
    private ApiFootballHttpClient footballClient;

    @Mock
    private PlayerSyncTxService txService;

    @InjectMocks
    private PlayerSyncOrchestrationService orchestrationService;

    private JsonMapper jsonMapper = new JsonMapper();

    @Test
    @DisplayName("외부 API 선수 정보를 DTO로 변환하여 TxService에 전달한다")
    void syncPlayer() throws Exception {
        // given
        String responseString = """
        {
            "id": 100,
            "name": "Son Heung-min"
        }
        """;

        Player player = Player.of("Son Heung-min", 100L);
        PlayerSaveResponseDto txResponse = PlayerSaveResponseDto.of(player);

        JsonNode responseJsonNode = jsonMapper.readTree(responseString);

        given(footballClient.callExternalPlayerApi(100L))
                .willReturn(responseJsonNode);

        PlayerSyncData data = new PlayerSyncData(responseJsonNode.get("id").asLong(), responseJsonNode.get("name").asString());

        given(txService.getOrCreatePlayerResponse(data))
                .willReturn(txResponse);

        // when
        PlayerSaveResponseDto response = orchestrationService.syncPlayer(100L);

        // then
        assertThat(response).isSameAs(txResponse);
        assertThat(response.getPlayerAPIId()).isEqualTo(100L);
        assertThat(response.getPlayerName()).isEqualTo("Son Heung-min");

        verify(footballClient, times(1)).callExternalPlayerApi(100L);
        verify(txService, times(1)).getOrCreatePlayerResponse(data);
    }


}
