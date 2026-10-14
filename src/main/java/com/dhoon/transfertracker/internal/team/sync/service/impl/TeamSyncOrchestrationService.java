package com.dhoon.transfertracker.internal.team.sync.service.impl;

import com.dhoon.transfertracker.external.football.client.ApiFootballHttpClient;
import com.dhoon.transfertracker.external.football.dto.LeagueTeamsResponseDto;
import com.dhoon.transfertracker.external.football.dto.TeamSaveResponseDto;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.query.dto.response.TeamItemResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class TeamSyncOrchestrationService {

    private final TeamSyncTxService txService;
    private final ApiFootballHttpClient apiFootballHttpClient;


    /**
     * 외부 API 를 호출해서, 해당 팀 데이터를 DB 에 저장하는 작업.
     * @param teamApiId -> 외부 API ID
     */
    public TeamSaveResponseDto syncTeam(Long teamApiId) {
        JsonNode teamData = apiFootballHttpClient.callExternalTeamApi(teamApiId);

        return txService.getOrCreateTeamResponse(teamData);
    }

    /**
     * 외부 API를 호출해서, 해당 리그에 해당 시즌에 속했던 팀들을 가지고 오고, 해당 팀들을 리그에 배치합니다.
     * @param leagueCode -> ENUM
     */
    public LeagueTeamsResponseDto syncLeagueTeams(LeagueCode leagueCode) {
        JsonNode node = apiFootballHttpClient.callExternalLeagueTeamsApi(leagueCode);

        List<TeamItemResponseDto> teams = new ArrayList<>();

        // 2024 년 기준으로 설정합니다. 리그 처음 만들때에만 설정하고, 이후에는 실행하지않는것을 권장합니다.
        node.get("response")
                .forEach(responseData -> {
                    JsonNode teamData = responseData.get("team");
                    teams.add(txService.getOrCreateTeamAndAssignLeagueResponse(teamData, leagueCode));
                });

        return LeagueTeamsResponseDto.of(leagueCode, teams);
    }

}
