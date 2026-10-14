package com.dhoon.transfertracker.internal.team.sync.service.impl;

import com.dhoon.transfertracker.external.football.dto.TeamSaveResponseDto;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.query.dto.response.TeamItemResponseDto;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

@RequiredArgsConstructor
@Slf4j
@Transactional
@Service
public class TeamSyncTxService {

    private final TeamRepository teamRepository;


    public TeamSaveResponseDto getOrCreateTeamResponse(JsonNode teamData) {
        Team team = getOrCreateTeam(teamData);
        return TeamSaveResponseDto.of(team);
    }


    private Team getOrCreateTeam(JsonNode teamData) {
        long teamApiId = teamData.get("id").asLong();

        return teamRepository.findByApiFootballId(teamApiId)
                .orElseGet(() -> teamRepository.save(
                        Team.of(
                                teamData.get("name").asString(),
                                teamData.get("id").asLong()
                        )
                ));
    }


    // 실행하지 않기를 권장합니다. ( 2024 년 기준 데이터이므로, 새로만든 리그가 아니라, 기존에 있던 리그면 팀 데이터가 왜곡됨.)
    public TeamItemResponseDto getOrCreateTeamAndAssignLeagueResponse(JsonNode teamData, LeagueCode leagueCode) {
        TeamItemResponseDto responseDto = getOrCreateTeamAndAssignLeague(teamData, leagueCode);

        return responseDto;
    }

    private TeamItemResponseDto getOrCreateTeamAndAssignLeague(JsonNode teamData, LeagueCode leagueCode) {
        Team team = getOrCreateTeam(teamData);
        team.assignTeamLeague(leagueCode);

        return TeamItemResponseDto.of(team);
    }
}
