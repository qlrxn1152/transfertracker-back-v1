package com.dhoon.transfertracker.internal.transfer.sync.service.impl;

import com.dhoon.transfertracker.external.football.client.ApiFootballHttpClient;
import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.transfer.domain.Transfer;
import com.dhoon.transfertracker.internal.transfer.query.dto.response.PlayerTransferItemResponseDto;
import com.dhoon.transfertracker.internal.transfer.query.dto.response.PlayerTransfersResponseDto;
import com.dhoon.transfertracker.internal.transfer.sync.dto.response.TeamTransfersSaveResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@Slf4j
@ExtendWith(MockitoExtension.class)
class TransferSyncOrchestrationServiceTest {

    @Mock
    private ApiFootballHttpClient apiFootballHttpClient;

    @Mock
    private TransferSyncTxService txService;

    @InjectMocks
    private TransferSyncOrchestrationService orchestrationService;

    JsonMapper jsonMapper = new JsonMapper();

    @Test
    @DisplayName("외부 API에서 조회한 선수 이적 정보를 TxService에 전달한다.")
    void syncPlayerTransfers_success() {
        // given
        Player player = Player.of("Messi", 111L);
        JsonNode node = createPlayerTransfersResponse();
        JsonNode playerData = node.get("response").get(0).get("player");
        JsonNode transferDatas = node.get("response").get(0).get("transfers");

        PlayerTransfersResponseDto expected = PlayerTransfersResponseDto.of(player, List.of());

        given(apiFootballHttpClient.callExternalPlayerTransferApi(111L))
                .willReturn(node);


        given(txService.getOrCreatePlayerTransfersResponse(playerData, transferDatas))
                .willReturn(expected);

        // when
        PlayerTransfersResponseDto response = orchestrationService.syncPlayerTransfers(111L);

        // then
        assertThat(response).isSameAs(expected);

        verify(txService, times(1)).getOrCreatePlayerTransfersResponse(playerData, transferDatas);
        verify(apiFootballHttpClient, times(1)).callExternalPlayerTransferApi(111L);
    }

    @Test
    @DisplayName("팀의 선수별 이적 정보를 조회하여 TxService에 전달한다.")
    void syncTeamTransfers_success() {
        // given
        JsonNode node = getTeamTransfersNode();
        JsonNode responses = node.get("response");

        JsonNode messiPlayerData = responses.get(0).get("player");
        JsonNode messiTransfers = responses.get(0).get("transfers");

        JsonNode brunoPlayerData = responses.get(1).get("player");
        JsonNode brunoTransfers = responses.get(1).get("transfers");

        given(txService.getDisplayTeamName(44L))
                .willReturn("바르셀로나");

        given(apiFootballHttpClient.callExternalTeamTransfersApi(44L))
                .willReturn(node);

        given(txService.getOrCreatePlayerTransfersResponse(messiPlayerData, messiTransfers))
                .willReturn(PlayerTransfersResponseDto.of(Player.of("Messi", 111L), List.of()));

        given(txService.getOrCreatePlayerTransfersResponse(brunoPlayerData, brunoTransfers))
                .willReturn(PlayerTransfersResponseDto.of(Player.of("Bruno Fernandes", 222L), List.of()));

        // 4. 선수별 TxService Stubbing

        // when
        TeamTransfersSaveResponseDto response = orchestrationService.syncTeamTransfers(44L);

        // then
        assertThat(response.getTeamName()).isEqualTo("바르셀로나");
        assertThat(response.getPlayersTransfers()).hasSize(2);
        assertThat(response.getPlayersTransfers()).extracting(PlayerTransfersResponseDto::getPlayerName)
                        .containsExactly("Messi", "Bruno Fernandes");

        verify(txService, times(1)).getDisplayTeamName(44L);
        verify(apiFootballHttpClient, times(1)).callExternalTeamTransfersApi(44L);
        verify(txService, times(2)).getOrCreatePlayerTransfersResponse(any(), any());

        verify(txService, times(1)).getOrCreatePlayerTransfersResponse(brunoPlayerData, brunoTransfers);
        verify(txService, times(1)).getOrCreatePlayerTransfersResponse(messiPlayerData, messiTransfers);
    }

    @Test
    @DisplayName("선수 한 명의 날짜 파싱에 실패해도 다음 선수를 동기화한다")
    void syncTeamTransfers_dateParseFailure_continuesNextPlayer() {
        Long teamApiId = 10L;
        String teamName = "Barcelona";
        String responseJson = """
        {
          "response": [
            {
              "player": {
                "id": 111,
                "name": "Messi"
              },
              "transfers": [
                {
                  "date": "2026-07-01",
                  "type": "Transfer",
                  "teams": {
                    "in": {
                      "id": 10,
                      "name": "Barcelona"
                    },
                    "out": {
                      "id": 20,
                      "name": "PSG"
                    }
                  }
                }
              ]
            },
            {
              "player": {
                "id": 222,
                "name": "Neymar"
              },
              "transfers": [
                {
                  "date": "invalid-date", 
                  "type": "Transfer",
                  "teams": {
                    "in": {
                      "id": 20,
                      "name": "PSG"
                    },
                    "out": {
                      "id": 10,
                      "name": "Barcelona"
                    }
                  }
                }
              ]
            },
            {
              "player": {
                "id": 333,
                "name": "Ronaldo"
              },
              "transfers": [
                {
                  "date": "2026-08-15",
                  "type": "Transfer",
                  "teams": {
                    "in": {
                      "id": 30,
                      "name": "Al Nassr"
                    },
                    "out": {
                      "id": 40,
                      "name": "Manchester United"
                    }
                  }
                }
              ]
            }
          ]
        }
        """;

        JsonNode responseNode = jsonMapper.readTree(responseJson);

        JsonNode neymarPlayer = responseNode.get("response").get(1).get("player");
        JsonNode neymarTransfer = responseNode.get("response").get(1).get("transfers");

        JsonNode messiPlayer = responseNode.get("response").get(0).get("player");
        JsonNode messiTransfer = responseNode.get("response").get(0).get("transfers");

        JsonNode ronaldoPlayer = responseNode.get("response").get(2).get("player");
        JsonNode ronaldoTransfer = responseNode.get("response").get(2).get("transfers");


        given(txService.getOrCreatePlayerTransfersResponse(neymarPlayer, neymarTransfer))
                .willThrow(new DateTimeParseException("잘못된 이적 날짜", "invalid-date", 0));

        PlayerTransfersResponseDto messiResponse = mock(PlayerTransfersResponseDto.class);
        PlayerTransfersResponseDto ronaldoResponse = mock(PlayerTransfersResponseDto.class);

        given(txService.getOrCreatePlayerTransfersResponse(messiPlayer, messiTransfer))
                .willReturn(messiResponse);

        given(txService.getOrCreatePlayerTransfersResponse(ronaldoPlayer, ronaldoTransfer))
                .willReturn(ronaldoResponse);

        given(txService.getDisplayTeamName(10L))
                .willReturn(teamName);

        given(apiFootballHttpClient.callExternalTeamTransfersApi(teamApiId))
                .willReturn(responseNode);

        // when
        TeamTransfersSaveResponseDto response = orchestrationService.syncTeamTransfers(teamApiId);

        // then
        assertThat(response.getSuccessPlayerCount()).isEqualTo(2);
        assertThat(response.getFailedPlayerCount()).isEqualTo(1);
        assertThat(response.getTotalPlayerCount()).isEqualTo(3);
        assertThat(response.getFailedPlayerIds()).contains(222L);

        assertThat(response.getPlayersTransfers()).hasSize(2);

        assertThat(response.getPlayersTransfers())
                .containsExactly(
                        messiResponse,
                        ronaldoResponse
                );


        verify(txService, times(1)).getOrCreatePlayerTransfersResponse(ronaldoPlayer, ronaldoTransfer);

    }







    private JsonNode createPlayerTransfersResponse() {

        return jsonMapper.readTree("""
    {
      "response": [
        {
          "player": {
            "id": 111,
            "name": "Messi"
          },
          "transfers": [
            {
              "date": "2026-07-01",
              "type": "Free",
              "teams": {
                "in": {
                  "id": 44,
                  "name": "Barcelona"
                },
                "out": {
                  "id": 33,
                  "name": "PSG"
                }
              }
            }
          ]
        }
      ]
    }
    """);
    }


    private JsonNode getTeamTransfersNode() {
        return jsonMapper.readTree("""
        {
          "response": [
            {
              "player": {
                "id": 111,
                "name": "Messi"
              },
              "transfers": [
                {
                  "date": "2026-07-01",
                  "type": "Free",
                  "teams": {
                    "in": {
                      "id": 44,
                      "name": "Barcelona"
                    },
                    "out": {
                      "id": 33,
                      "name": "PSG"
                    }
                  }
                }
              ]
            },
            {
              "player": {
                "id": 222,
                "name": "Bruno Fernandes"
              },
              "transfers": [
                {
                  "date": "2026-08-15",
                  "type": "Transfer",
                  "teams": {
                    "in": {
                      "id": 44,
                      "name": "Barcelona"
                    },
                    "out": {
                      "id": 55,
                      "name": "Manchester United"
                    }
                  }
                },
                {
                  "date": "2024-07-01",
                  "type": "Transfer",
                  "teams": {
                    "in": {
                      "id": 55,
                      "name": "Manchester United"
                    },
                    "out": {
                      "id": 66,
                      "name": "Sporting CP"
                    }
                  }
                }
              ]
            }
          ]
        }
        """);
    }

}