package com.dhoon.transfertracker.internal.player.sync.service.impl;

import com.dhoon.transfertracker.external.football.client.ApiFootballHttpClient;
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
     * 외부 API 를 호출해서, 해당 선수 데이터를 DB 에 저장하는 작업.
     * @param playerApiId -> 외부 API ID
     */
    public PlayerSaveResponseDto syncPlayer(Long playerApiId) {
        JsonNode playerData = footballClient.callExternalPlayerApi(playerApiId);

        return txService.getOrCreatePlayerResponse(playerData);
    }



}
