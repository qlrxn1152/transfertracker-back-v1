package com.dhoon.transfertracker.internal.team.service.impl;

import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.dto.response.TeamItemResponseDto;
import com.dhoon.transfertracker.internal.team.dto.response.TeamPageInfosResponseDto;
import com.dhoon.transfertracker.internal.team.dto.response.TeamsResponseDto;
import com.dhoon.transfertracker.internal.team.exception.InvalidLeagueCodeValueException;
import com.dhoon.transfertracker.internal.team.service.TeamService;
import com.dhoon.transfertracker.internal.team.exception.InvalidTeamIdException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class TeamOrchestrationService {

    private final TeamService teamTxService;

    public TeamItemResponseDto getTeam(Long teamId) {
        validateTeamId(teamId);

        return teamTxService.getTeam(teamId);
    }

    public TeamsResponseDto getTeams() {
        return teamTxService.getTeams();
    }

    public TeamsResponseDto getLeagueTeams(LeagueCode leagueCode) {
        validateLeagueCode(leagueCode);
        return teamTxService.getLeagueTeams(leagueCode);
    }

    public TeamPageInfosResponseDto getTeamInfo(Long teamId) {
        validateTeamId(teamId);
        return teamTxService.getTeamInfo(teamId);
    }






    public void validateTeamId(Long teamId) {
        if (teamId == null) {
            throw new InvalidTeamIdException();
        }
    }

    public void validateLeagueCode(LeagueCode leagueCode) {
        if (leagueCode == null) {
            throw new InvalidLeagueCodeValueException();
        }
    }



}
