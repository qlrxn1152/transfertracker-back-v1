package com.dhoon.transfertracker.external.football.service;

import com.dhoon.transfertracker.external.football.client.ApiFootballHttpClient;
import com.dhoon.transfertracker.external.football.dto.*;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.dto.response.TeamItemResponseDto;
import com.dhoon.transfertracker.internal.transfer.query.dto.response.PlayerTransfersResponseDto;
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

    /**
     * 외부 API 를 호출해서, 해당 팀 데이터를 DB 에 저장하는 작업.
     * @param teamApiId -> 외부 API ID
     */
    public TeamSaveResponseDto syncTeam(Long teamApiId) {
        JsonNode teamData = footballRestClient.callExternalTeamApi(teamApiId);

        return footballSyncTxService.getOrCreateTeamResponse(teamData);
    }


    /**
     * 외부 API 를 호출해서, 해당 팀의 선수들을 DB 에 저장하는 작업.
     * @param teamApiId -> 외부 API ID
     */
    public TeamPlayersSaveResponseDto syncTeamPlayers(Long teamApiId) {
        JsonNode node = footballRestClient.callExternalTeamPlayersApi(teamApiId);
        JsonNode teamData = node.get("response").get(0).get("team");
        JsonNode playersData = node.get("response").get(0).get("players");

        List<TeamPlayerSaveResponseDto> response = new ArrayList<>();

        playersData.forEach(player -> {
            TeamPlayerSaveResponseDto saveTeamPlayerResponse = footballSyncTxService.getOrCreateTeamPlayerResponse(player, teamData);

            response.add(saveTeamPlayerResponse);
        });

        return TeamPlayersSaveResponseDto.of(response);
    }



    /**
     * 외부 API를 호출해서, 해당 리그에 해당 시즌에 속했던 팀들을 가지고 오고, 해당 팀들을 리그에 배치합니다.
     * @param leagueCode -> ENUM
     */
    public LeagueTeamsResponseDto syncLeagueTeams(LeagueCode leagueCode) {
        JsonNode node = footballRestClient.callExternalLeagueTeamsApi(leagueCode);

        List<TeamItemResponseDto> teams = new ArrayList<>();

        // 2024 년 기준으로 설정합니다. 리그 처음 만들때에만 설정하고, 이후에는 실행하지않는것을 권장합니다.
        node.get("response")
                .forEach(responseData -> {
                    JsonNode teamData = responseData.get("team");
                    teams.add(footballSyncTxService.getOrCreateTeamAndAssignLeagueResponse(teamData, leagueCode));
                });

        return LeagueTeamsResponseDto.of(leagueCode, teams);
    }





}
