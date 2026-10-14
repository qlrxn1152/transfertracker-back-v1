package com.dhoon.transfertracker.internal.transfer.sync.service.impl;

import com.dhoon.transfertracker.external.football.client.ApiFootballHttpClient;
import com.dhoon.transfertracker.internal.transfer.query.dto.response.PlayerTransfersResponseDto;
import com.dhoon.transfertracker.internal.transfer.sync.dto.response.TeamTransfersSaveResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
public class TransferSyncOrchestrationService {

    private final TransferSyncTxService txService;
    private final ApiFootballHttpClient apiFootballHttpClient;

    /**
     * 외부 API 를 호출해서, 해당 선수의 이적 데이터를 DB 에 저장하는 작업.
     * @param playerApiId -> 외부 API ID
     */
    public PlayerTransfersResponseDto syncPlayerTransfers(Long playerApiId) {
        JsonNode node = apiFootballHttpClient.callExternalPlayerTransferApi(playerApiId);

        JsonNode playerData = node.get("response").get(0).get("player");
        JsonNode transferDatas = node.get("response").get(0).get("transfers");

        return txService.getOrCreatePlayerTransfersResponse(playerData, transferDatas);
    }


    /**
     * 외부 API 를 호출해서, 해당 팀의 선수들을 DB 에 저장하는 작업.
     * @param teamApiId -> 외부 API ID
     */
    public TeamTransfersSaveResponseDto syncTeamTransfers(Long teamApiId) {
        String teamName = txService.getDisplayTeamName(teamApiId);

        JsonNode node = apiFootballHttpClient.callExternalTeamTransfersApi(teamApiId);

        JsonNode responses = node.get("response");

        List<PlayerTransfersResponseDto> playerTransfers = new ArrayList<>();

        // playerTransfer -> 1명의 선수의 이적 데이터.
        responses.forEach(playerTransfer -> {
            JsonNode playerData = playerTransfer.get("player");
            JsonNode transferData = playerTransfer.get("transfers");

            playerTransfers.add(txService.getOrCreatePlayerTransfersResponse(playerData, transferData));
        });


        return TeamTransfersSaveResponseDto.of(teamName, playerTransfers);
    }





}
