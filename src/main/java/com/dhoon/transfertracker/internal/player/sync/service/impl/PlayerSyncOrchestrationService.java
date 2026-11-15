package com.dhoon.transfertracker.internal.player.sync.service.impl;

import com.dhoon.transfertracker.external.football.client.ApiFootballHttpClient;
import com.dhoon.transfertracker.internal.player.sync.dto.PlayerSyncData;
import com.dhoon.transfertracker.internal.player.sync.dto.response.PlayerSaveResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

@Service
@Slf4j
@RequiredArgsConstructor
public class PlayerSyncOrchestrationService {

    private final PlayerSyncTxService txService;
    private final ApiFootballHttpClient footballClient;

    /**
     * API-Football에서 선수 정보를 조회하고 내부 DB에 동기화한다.
     *
     * <p>외부 API에서 선수 정보를 조회하고 필요한 데이터를 변환한 후,
     * 트랜잭션 서비스에 저장을 위임한다.
     * 동일한 API-Football ID의 선수가 존재하면 기존 정보를 반환하고,
     * 존재하지 않으면 신규 등록한다.</p>
     *
     * @param playerApiId API-Football 선수 식별자
     * @return 기존 또는 신규 등록된 선수 정보
     */
    public PlayerSaveResponseDto syncPlayer(Long playerApiId) {

        validatePlayerApiId(playerApiId);

        JsonNode playerData = footballClient.callExternalPlayerApi(playerApiId);

        validateExternalResponseData(playerData);

        JsonNode idNode = playerData.get("id");
        JsonNode nameNode = playerData.get("name");

        validateIdNode(idNode);
        validateNameNode(nameNode);

        PlayerSyncData playerSyncData = new PlayerSyncData(
                idNode.asLong(),
                nameNode.asString()
        );

        return txService.getOrCreatePlayerResponse(playerSyncData);
    }



    private static void validateExternalResponseData(JsonNode playerData) {
        if (playerData == null || !playerData.isObject()) {
            throw new IllegalArgumentException("외부 API 응답에서 선수 데이터가 올바르지 않습니다.");
        }
    }

    private void validatePlayerApiId(Long playerApiId) {
        if (playerApiId == null || playerApiId <= 0) {
            throw new IllegalArgumentException("선수의 API ID 값은 자연수이어야합니다.");
        }
    }


    private void validateNameNode(JsonNode nameNode) {

        if (nameNode == null || !nameNode.isString()) {
            throw new IllegalArgumentException("외부 API 응답에서, [Name] 의 값이 잘못된 값입니다.");
        }

        String strippedPlayerName = nameNode.asString().strip();
        if (strippedPlayerName.isBlank() || strippedPlayerName.equalsIgnoreCase("null")) {
            throw new IllegalArgumentException("외부 API 응답에서, [Name] 의 값이 잘못된 값입니다.");
        }

    }

    private void validateIdNode(JsonNode idNode) {
        if (idNode == null || idNode.isNull() || !idNode.isIntegralNumber() || !idNode.canConvertToLong() || idNode.asLong() <= 0) {
            throw new IllegalArgumentException("외부 API 응답에서, [Api ID] 의 값이 잘못된 값입니다.");
        }
    }


}
