package com.dhoon.transfertracker.external.football.service;

import com.dhoon.transfertracker.external.football.client.ApiFootballHttpClient;
import com.dhoon.transfertracker.external.football.dto.*;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.query.dto.response.TeamItemResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
public class FootballSyncService {

    private final ApiFootballHttpClient footballRestClient;
    private final FootballSyncTxService footballSyncTxService;


    /**
     * 외부 API 를 호출해서, 해당 선수 데이터를 DB 에 저장하는 작업.
     * @param playerApiId -> 외부 API ID
     */
    public PlayerSaveResponseDto syncPlayer(Long playerApiId) {
        JsonNode playerData = footballRestClient.callExternalPlayerApi(playerApiId);

        return footballSyncTxService.getOrCreatePlayerResponse(playerData);
    }





}
