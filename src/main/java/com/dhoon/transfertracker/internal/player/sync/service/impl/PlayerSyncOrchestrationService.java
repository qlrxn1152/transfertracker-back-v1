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
        JsonNode playerData = footballClient.callExternalPlayerApi(playerApiId);

        if (playerData.get("id") == null || playerData.get("name") == null) {
            throw new IllegalStateException("외부 API 선수 정보에 필수 필드가 없습니다.");
        }

        PlayerSyncData playerSyncData = new PlayerSyncData(
                playerData.get("id").asLong(),
                playerData.get("name").asString()
        );

        return txService.getOrCreatePlayerResponse(playerSyncData);
    }





}
