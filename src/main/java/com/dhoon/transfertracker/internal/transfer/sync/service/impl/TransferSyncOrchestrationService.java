package com.dhoon.transfertracker.internal.transfer.sync.service.impl;

import com.dhoon.transfertracker.external.football.client.ApiFootballHttpClient;
import com.dhoon.transfertracker.internal.transfer.query.dto.response.PlayerTransfersResponseDto;
import com.dhoon.transfertracker.internal.transfer.sync.dto.response.TeamTransfersSaveResponseDto;
import com.dhoon.transfertracker.internal.transfer.sync.exception.InvalidExternalApiResponseException;
import com.dhoon.transfertracker.internal.transfer.sync.exception.InvalidPlayerTransferDataException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

        JsonNode responses = callExternalTeamTransferApiAndReturnResponse(teamApiId);

        List<PlayerTransfersResponseDto> successesPlayerTransfers = new ArrayList<>();
        Set<Long> failedPlayerIds = new HashSet<>();
        int totalPlayerCount = responses.size();

        syncTeamPlayerTransfers(teamApiId, responses, successesPlayerTransfers, failedPlayerIds);
        return TeamTransfersSaveResponseDto.of(teamName, successesPlayerTransfers, failedPlayerIds, totalPlayerCount);
    }


    //// ====================== ////
    private JsonNode callExternalTeamTransferApiAndReturnResponse(Long teamApiId) {
        JsonNode node = apiFootballHttpClient.callExternalTeamTransfersApi(teamApiId);

        if (node == null || !node.isObject()) {
            throw new InvalidExternalApiResponseException();
        }

        JsonNode responses = node.get("response");

        if (responses == null || !responses.isArray()) {
            throw new InvalidExternalApiResponseException();
        }

        return responses;
    }


    ////  ================================================== ////
    private void syncTeamPlayerTransfers(Long teamApiId, JsonNode responses, List<PlayerTransfersResponseDto> successesPlayerTransfers, Set<Long> failedPlayerIds) {
        // playerTransfer -> 1명의 선수의 이적 데이터.
        responses.forEach(playerTransfer -> {

            Long playerApiId = null;

            try {
                playerApiId = validatePlayerJsonNodeAndExtractPlayerApiId(playerTransfer);

                validatePlayerTransfers(playerTransfer);

                JsonNode playerData = playerTransfer.get("player");
                JsonNode transferData = playerTransfer.get("transfers");

                successesPlayerTransfers.add(txService.getOrCreatePlayerTransfersResponse(playerData, transferData));
            }
            catch (InvalidPlayerTransferDataException e) {
                if (playerApiId != null) {
                    failedPlayerIds.add(playerApiId);
                }

                log.warn(
                        "외부 Football API 응답데이터 형식 오류로 인한 선수 이적 동기화 실패. teamApiId={}, playerApiId={}",
                        teamApiId,
                        playerApiId,
                        e
                );
            }
            catch (DateTimeParseException e) {
                if (playerApiId != null) {
                    failedPlayerIds.add(playerApiId);
                }


                log.warn(
                        "선수 이적 동기화 실패. teamApiId={}, playerApiId={}",
                        teamApiId,
                        playerApiId,
                        e
                );
            }
            catch (DataAccessResourceFailureException e) {
                log.error(
                        "DB 접근 장애로 팀 동기화 중단. teamApiId={}, playerApiId={}",
                        teamApiId,
                        playerApiId,
                        e
                );

                throw e;
            }

        });
    }




    private void validatePlayerTransfers(JsonNode playerTransfer) {
        JsonNode transfers = playerTransfer.get("transfers");

        if (transfers == null || !transfers.isArray()) {
            throw new InvalidPlayerTransferDataException();
        }
    }

    private void validatePlayerData(JsonNode playerTransfer) {
        JsonNode player = playerTransfer.get("player");

        if (player == null || !player.isObject()) {
            throw new InvalidPlayerTransferDataException();
        }

        JsonNode id = player.get("id");

        if (id == null
                || !id.isIntegralNumber()
                || !id.canConvertToLong()
                || id.asLong() <= 0) {
            throw new InvalidPlayerTransferDataException();
        }
    }

    private void validateExternalPlayerTransferResponse(JsonNode playerTransfer) {
        if (playerTransfer == null || !playerTransfer.isObject()) {
            throw new InvalidPlayerTransferDataException();
        }
    }

    private Long validatePlayerJsonNodeAndExtractPlayerApiId(JsonNode playerTransfer) {
        validateExternalPlayerTransferResponse(playerTransfer);
        validatePlayerData(playerTransfer);

        return playerTransfer.get("player").get("id").asLong();
    }


}
