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

    // 기존 Controller 호환
    public TeamPlayersSaveResponseDto syncTeamPlayers(Long teamApiId) {

        JsonNode responseData = fetchSquad(teamApiId);

        return saveTeamPlayers(responseData);
    }

    public int syncNationalTeamPlayers(Long teamApiId) {

        JsonNode responseData = fetchSquad(teamApiId);

        return saveNationalTeamPlayers(responseData);
    }

    // 외부 API 호출만 담당
    public JsonNode fetchSquad(Long teamApiId) {

        JsonNode root = footballHttpClient
                .callExternalTeamPlayersApi(teamApiId);

        if (root == null
                || !root.has("errors")
                || !root.path("errors").isEmpty()
                || !root.path("response").isArray()
                || root.path("response").size() != 1) {

            throw new IllegalStateException(
                    "유효하지 않은 선수단 응답. teamApiId="
                            + teamApiId
            );
        }

        JsonNode responseData = root.path("response").get(0);

        JsonNode teamData = responseData.path("team");
        JsonNode playersData = responseData.path("players");

        if (teamData.path("id").asLong() != teamApiId
                || !playersData.isArray()
                || playersData.isEmpty()) {

            throw new IllegalStateException(
                    "유효하지 않은 선수단 정보. teamApiId="
                            + teamApiId
            );
        }

        for (JsonNode player : playersData) {

            if (!hasValidPlayerId(player)
                    || !player.path("name").isTextual()
                    || player.path("name").asText().isBlank()) {

                throw new IllegalStateException(
                        "유효하지 않은 선수 정보. teamApiId="
                                + teamApiId
                );
            }
        }

        return responseData;
    }

    // 클럽 선수단 DB 저장
    public TeamPlayersSaveResponseDto saveTeamPlayers(
            JsonNode responseData
    ) {

        JsonNode teamData = responseData.path("team");
        JsonNode playersData = responseData.path("players");

        List<TeamPlayerSaveResponseDto> responses =
                new ArrayList<>();

        for (JsonNode player : playersData) {

            responses.add(
                    txService.getOrCreateTeamPlayerResponse(
                            player,
                            teamData
                    )
            );
        }

        return TeamPlayersSaveResponseDto.of(responses);
    }

    // 국가대표 선수 DB 저장
    public int saveNationalTeamPlayers(JsonNode responseData) {

        JsonNode playersData = responseData.path("players");

        int successCount = 0;

        for (JsonNode player : playersData) {

            txService.saveNationalTeamPlayer(player);

            successCount++;
        }

        return successCount;
    }

    private boolean hasValidPlayerId(JsonNode player) {

        JsonNode id = player.get("id");

        return id != null
                && id.isIntegralNumber()
                && id.canConvertToLong()
                && id.asLong() > 0;
    }
}