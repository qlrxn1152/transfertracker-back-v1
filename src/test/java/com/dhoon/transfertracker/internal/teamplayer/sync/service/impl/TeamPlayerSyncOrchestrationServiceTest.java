package com.dhoon.transfertracker.internal.teamplayer.sync.service.impl;

import com.dhoon.transfertracker.external.football.client.ApiFootballHttpClient;
import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.teamplayer.domain.TeamPlayer;
import com.dhoon.transfertracker.internal.teamplayer.sync.dto.response.TeamPlayerSaveResponseDto;
import com.dhoon.transfertracker.internal.teamplayer.sync.dto.response.TeamPlayersSaveResponseDto;
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

@ExtendWith(MockitoExtension.class)
public class TeamPlayerSyncOrchestrationServiceTest {

    @Mock
    private TeamPlayerSyncTxService teamPlayerSyncTxService;

    @Mock
    private ApiFootballHttpClient footballHttpClient;

    @InjectMocks
    private TeamPlayerSyncOrchestrationService teamPlayerSyncOrchestrationService;

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    @DisplayName("외부 API에서 선수 목록을 받아 각 선수의 소속 관계를 동기화한다.")
    void syncTeamPlayers_success() throws Exception {
        // given
        JsonNode node = jsonMapper.readTree("""
        {
            "response": [
                {
                    "team": {
                        "id": 222,
                        "name": "Inter Miami"
                    },
                    "players": [
                        {
                            "id": 111,
                            "name": "Messi"
                        },
                        {
                            "id": 333,
                            "name": "Suarez"
                        }
                    ]
                }
            ]
        }
        """);

        JsonNode teamData = node.get("response").get(0).get("team");
        JsonNode playersData = node.get("response").get(0).get("players");

        JsonNode messiData = playersData.get(0);
        JsonNode suarezData = playersData.get(1);

        Team team = Team.of("Inter Miami", 222L);
        Player messi = Player.of("Messi", 111L);
        Player suarez = Player.of("Suarez", 333L);

        TeamPlayer teamPlayerMessi = TeamPlayer.of(team, messi);
        TeamPlayer teamPlayerSuarez = TeamPlayer.of(team, suarez);

        given(footballHttpClient.callExternalTeamPlayersApi(222L))
                .willReturn(node);

        given(teamPlayerSyncTxService.getOrCreateTeamPlayerResponse(messiData, teamData))
                .willReturn(TeamPlayerSaveResponseDto.of(teamPlayerMessi));

        given(teamPlayerSyncTxService.getOrCreateTeamPlayerResponse(suarezData, teamData))
                .willReturn(TeamPlayerSaveResponseDto.of(teamPlayerSuarez));


        // when
        TeamPlayersSaveResponseDto response = teamPlayerSyncOrchestrationService.syncTeamPlayers(222L);

        // then
        assertThat(response.getPlayers()).hasSize(2);
        assertThat(response.getPlayers()).extracting(TeamPlayerSaveResponseDto::getPlayerName)
                .containsExactlyInAnyOrder("Messi", "Suarez");

        assertThat(response.getPlayers()).extracting(TeamPlayerSaveResponseDto::getTeamName)
                .containsExactly("Inter Miami", "Inter Miami");

        verify(footballHttpClient, times(1)).callExternalTeamPlayersApi(222L);

        verify(teamPlayerSyncTxService, times(1)).getOrCreateTeamPlayerResponse(messiData, teamData);
        verify(teamPlayerSyncTxService, times(1)).getOrCreateTeamPlayerResponse(suarezData, teamData);
    }

    @Test
    @DisplayName("외부 API의 선수 목록이 비어 있으면 빈 목록을 반환한다.")
    void syncTeamPlayers_empty() {
        // given
        JsonNode node = jsonMapper.readTree("""
        {
            "response": [
                {
                    "team": {
                        "id": 222,
                        "name": "Inter Miami"
                    },
                    "players": []
                }
            ]
        }
        """);

        given(footballHttpClient.callExternalTeamPlayersApi(222L))
                .willReturn(node);

        // when
        TeamPlayersSaveResponseDto response = teamPlayerSyncOrchestrationService.syncTeamPlayers(222L);

        // then
        verify(footballHttpClient, times(1)).callExternalTeamPlayersApi(222L);

        verifyNoInteractions(teamPlayerSyncTxService);

        assertThat(response.getPlayers()).isEmpty();
    }
}
