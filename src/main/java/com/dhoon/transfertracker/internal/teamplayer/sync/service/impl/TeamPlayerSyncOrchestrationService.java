package com.dhoon.transfertracker.internal.teamplayer.sync.service.impl;

import com.dhoon.transfertracker.external.football.client.ApiFootballHttpClient;
import com.dhoon.transfertracker.internal.teamplayer.sync.dto.response.TeamPlayerSaveResponseDto;
import com.dhoon.transfertracker.internal.teamplayer.sync.dto.response.TeamPlayersSaveResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class TeamPlayerSyncOrchestrationService {

    private final TeamPlayerSyncTxService txService;
    private final ApiFootballHttpClient footballHttpClient;


    /**
     * 외부 API 를 호출해서, 해당 팀의 선수들을 DB 에 저장하는 작업.
     * @param teamApiId -> 외부 API ID
     */
    public TeamPlayersSaveResponseDto syncTeamPlayers(Long teamApiId) {
        JsonNode node = footballHttpClient.callExternalTeamPlayersApi(teamApiId);

        JsonNode teamData = node.get("response").get(0).get("team");
        JsonNode playersData = node.get("response").get(0).get("players");

        List<TeamPlayerSaveResponseDto> response = new ArrayList<>();

        playersData.forEach(player -> {
            TeamPlayerSaveResponseDto saveTeamPlayerResponse = txService.getOrCreateTeamPlayerResponse(player, teamData);

            response.add(saveTeamPlayerResponse);
        });

        return TeamPlayersSaveResponseDto.of(response);
    }

}
