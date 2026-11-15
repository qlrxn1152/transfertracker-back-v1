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
        PlayerSaveResponseDto expected = PlayerSaveResponseDto.of(player);

        JsonNode responseJsonNode = jsonMapper.readTree(responseString);

        given(footballClient.callExternalPlayerApi(100L))
                .willReturn(responseJsonNode);

        PlayerSyncData playerSyncData = new PlayerSyncData(
                responseJsonNode.get("id").asLong(),
                responseJsonNode.get("name").asString()
        );

        given(txService.getOrCreatePlayerResponse(playerSyncData))
                .willReturn(expected);

        // when
        PlayerSaveResponseDto response = orchestrationService.syncPlayer(100L);

        // then
        assertThat(response).isSameAs(expected);
        assertThat(response.getPlayerAPIId()).isEqualTo(100L);
        assertThat(response.getPlayerName()).isEqualTo("Son Heung-min");

        verify(footballClient, times(1)).callExternalPlayerApi(100L);
        verify(txService, times(1)).getOrCreatePlayerResponse(playerSyncData);
    }

    @Test
    @DisplayName(value = "playerApiId 가 null 이거나, 자연수가 아닌경우 예외가 발생한다.")
    void syncPlayer_fail_playerApiId() throws Exception {
        // given
        Long playerApiIdNull = null;
        Long playerApiIdNegative = Long.valueOf(-2);
        Long playerApiIdZero = Long.valueOf(0);

        // when && then
        assertThatThrownBy(() -> orchestrationService.syncPlayer(playerApiIdNull))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> orchestrationService.syncPlayer(playerApiIdNegative))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> orchestrationService.syncPlayer(playerApiIdZero))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(txService);
    }

    @Test
    @DisplayName("외부 API 응답에서, id 가 존재하지 않는경우, 예외가 발생한다.")
    void syncPlayer_fail_externalResponse_apiId_null() throws Exception {
        // given
        String responseString = """ 
        {
            "id": null,
            "name": "Son Heung-min"
        }
        """;

        JsonNode responseJsonNode = jsonMapper.readTree(responseString);


        given(footballClient.callExternalPlayerApi(100L))
                .willReturn(responseJsonNode);

        // when && then
        assertThatThrownBy(() -> orchestrationService.syncPlayer(100L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("외부 API 응답에서, [Api ID] 의 값이 잘못된 값입니다.");
    }

    @Test
    @DisplayName("외부 API 응답에서, id 필드 자체가 존재하지 않는경우, 예외가 발생한다.")
    void syncPlayer_fail_externalResponse_apiId_fieldEmtpy() throws Exception {
        // given
        String responseString = """ 
        {
            "name": "Son Heung-min"
        }
        """;

        JsonNode responseJsonNode = jsonMapper.readTree(responseString);


        given(footballClient.callExternalPlayerApi(100L))
                .willReturn(responseJsonNode);

        // when && then
        assertThatThrownBy(() -> orchestrationService.syncPlayer(100L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("외부 API 응답에서, [Api ID] 의 값이 잘못된 값입니다.");
    }

    @Test
    @DisplayName("외부 API 응답에서, id 의 값이 정수형이 아닌경우 예외가 발생한다.")
    void syncPlayer_fail_externalResponse_apiId_StringNull() throws Exception {
        // given
        String responseString = """ 
        {
            "id": "null",
            "name": "Son Heung-min"
        }
        """;

        JsonNode responseJsonNode = jsonMapper.readTree(responseString);


        given(footballClient.callExternalPlayerApi(100L))
                .willReturn(responseJsonNode);

        // when && then
        assertThatThrownBy(() -> orchestrationService.syncPlayer(100L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("외부 API 응답에서, [Api ID] 의 값이 잘못된 값입니다.");
    }

    @Test
    @DisplayName("외부 API 응답에서, id 의 값이 정수형이 아닌경우 ( String null )  예외가 발생한다.")
    void syncPlayer_fail_externalResponse_apiId_float_double() throws Exception {
        // given
        String responseString = """ 
        {
            "id": 3.7123,
            "name": "Son Heung-min"
        }
        """;

        JsonNode responseJsonNode = jsonMapper.readTree(responseString);


        given(footballClient.callExternalPlayerApi(100L))
                .willReturn(responseJsonNode);

        // when && then
        assertThatThrownBy(() -> orchestrationService.syncPlayer(100L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("외부 API 응답에서, [Api ID] 의 값이 잘못된 값입니다.");
    }

    @Test
    @DisplayName("외부 API 응답에서, id 값이 0인경우, 예외가 발생한다.")
    void syncPlayer_fail_externalResponse_apiId_zero() throws Exception {
        // given
        String responseString = """ 
        {
            "id": 0,
            "name": "Son Heung-min"
        }
        """;

        JsonNode responseJsonNode = jsonMapper.readTree(responseString);

        given(footballClient.callExternalPlayerApi(100L))
                .willReturn(responseJsonNode);


        // when && then
        assertThatThrownBy(() -> orchestrationService.syncPlayer(100L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("외부 API 응답에서, [Api ID] 의 값이 잘못된 값입니다.");
    }

    @Test
    @DisplayName("외부 API 응답에서, id 값이 음수인경우, 예외가 발생한다.")
    void syncPlayer_fail_externalResponse_apiId_negative() throws Exception {
        // given
        String responseString = """ 
        {
            "id": -3,
            "name": "Son Heung-min"
        }
        """;

        JsonNode responseJsonNode = jsonMapper.readTree(responseString);

        given(footballClient.callExternalPlayerApi(100L))
                .willReturn(responseJsonNode);


        // when && then
        assertThatThrownBy(() -> orchestrationService.syncPlayer(100L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("외부 API 응답에서, [Api ID] 의 값이 잘못된 값입니다.");
    }

    @Test
    @DisplayName("외부 API 응답에서, id 값이 Long 타입의 범위를 넘어간경우, 예외가 발생한다.")
    void syncPlayer_fail_externalResponse_apiId_Exceeds_LongRange() throws Exception {
        // given
        String responseString = """ 
        {
            "id": 68498646216546874687,
            "name": "Son Heung-min"
        }
        """;

        JsonNode responseJsonNode = jsonMapper.readTree(responseString);

        given(footballClient.callExternalPlayerApi(100L))
                .willReturn(responseJsonNode);


        // when && then
        assertThatThrownBy(() -> orchestrationService.syncPlayer(100L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("외부 API 응답에서, [Api ID] 의 값이 잘못된 값입니다.");
    }

    @Test
    @DisplayName("외부 API 응답에서, name 필드가 존재하지 않는경우 예외가 발생한다.")
    void syncPlayer_fail_externalResponse_name_fieldEmtpy() throws Exception {
        // given
        String responseString = """ 
        {
            "id": 100
        }
        """;

        JsonNode responseJsonNode = jsonMapper.readTree(responseString);

        given(footballClient.callExternalPlayerApi(100L))
                .willReturn(responseJsonNode);


        // when && then
        assertThatThrownBy(() -> orchestrationService.syncPlayer(100L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("외부 API 응답에서, [Name] 의 값이 잘못된 값입니다.");
    }

    @Test
    @DisplayName("외부 API 응답에서, name 필드의 String 값이 비어져있는경우 예외가 발생한다.")
    void syncPlayer_fail_externalResponse_name_valueBlank() throws Exception {
        // given
        String responseString = """ 
        {
            "id": 100,
            "name": ""
        }
        """;

        JsonNode responseJsonNode = jsonMapper.readTree(responseString);

        given(footballClient.callExternalPlayerApi(100L))
                .willReturn(responseJsonNode);


        // when && then
        assertThatThrownBy(() -> orchestrationService.syncPlayer(100L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("외부 API 응답에서, [Name] 의 값이 잘못된 값입니다.");
    }

    @Test
    @DisplayName("외부 API 응답에서, name 필드의 String 값이 공백만 존재하는경우 예외가 발생한다.")
    void syncPlayer_fail_externalResponse_name_valueBlank_void() throws Exception {
        // given
        String responseString = """ 
        {
            "id": 100,
            "name": "           "
        }
        """;

        JsonNode responseJsonNode = jsonMapper.readTree(responseString);

        given(footballClient.callExternalPlayerApi(100L))
                .willReturn(responseJsonNode);


        // when && then
        assertThatThrownBy(() -> orchestrationService.syncPlayer(100L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("외부 API 응답에서, [Name] 의 값이 잘못된 값입니다.");
    }

    @Test
    @DisplayName("외부 API 응답에서, name 필드의 String 값이 대소문자 구분없이 문자열 null인경우 예외가 발생한다.")
    void syncPlayer_fail_externalResponse_name_StringNull() throws Exception {
        // given
        String responseString = """ 
        {
            "id": 100,
            "name": "nUll"
        }
        """;

        JsonNode responseJsonNode = jsonMapper.readTree(responseString);

        given(footballClient.callExternalPlayerApi(100L))
                .willReturn(responseJsonNode);


        // when && then
        assertThatThrownBy(() -> orchestrationService.syncPlayer(100L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("외부 API 응답에서, [Name] 의 값이 잘못된 값입니다.");
    }

    @Test
    @DisplayName("외부 API 응답에서, name 필드의 값이 null 인 경우, 예외가 발생한다.")
    void syncPlayer_fail_externalResponse_name_null() throws Exception {
        // given
        String responseString = """ 
        {
            "id": 100,
            "name": null
        }
        """;

        JsonNode responseJsonNode = jsonMapper.readTree(responseString);

        given(footballClient.callExternalPlayerApi(100L))
                .willReturn(responseJsonNode);


        // when && then
        assertThatThrownBy(() -> orchestrationService.syncPlayer(100L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("외부 API 응답에서, [Name] 의 값이 잘못된 값입니다.");
    }

    @Test
    @DisplayName("외부 API 응답에서, name 필드의 값이 숫자값인 경우, 예외가 발생한다.")
    void syncPlayer_fail_externalResponse_name_number() throws Exception {
        // given
        String responseString = """ 
        {
            "id": 100,
            "name": 123
        }
        """;

        JsonNode responseJsonNode = jsonMapper.readTree(responseString);

        given(footballClient.callExternalPlayerApi(100L))
                .willReturn(responseJsonNode);


        // when && then
        assertThatThrownBy(() -> orchestrationService.syncPlayer(100L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("외부 API 응답에서, [Name] 의 값이 잘못된 값입니다.");
    }

    @Test
    @DisplayName("외부 API 응답에서, name 필드의 값이 앞뒤로 공백을 포함한 문자열 대소문자 구분없이 null 인 경우, 예외가 발생한다.")
    void syncPlayer_fail_externalResponse_name_StringNullWithEmpty() throws Exception {
        // given
        String responseString = """ 
        {
            "id": 100,
            "name": "    nuLl "
        }
        """;

        JsonNode responseJsonNode = jsonMapper.readTree(responseString);

        given(footballClient.callExternalPlayerApi(100L))
                .willReturn(responseJsonNode);


        // when && then
        assertThatThrownBy(() -> orchestrationService.syncPlayer(100L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("외부 API 응답에서, [Name] 의 값이 잘못된 값입니다.");
    }













}
