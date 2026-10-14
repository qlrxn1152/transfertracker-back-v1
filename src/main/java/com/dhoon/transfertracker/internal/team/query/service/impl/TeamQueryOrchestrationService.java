package com.dhoon.transfertracker.internal.team.query.service.impl;

import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.query.dto.response.TeamItemResponseDto;
import com.dhoon.transfertracker.internal.team.query.dto.response.TeamPageInfosResponseDto;
import com.dhoon.transfertracker.internal.team.query.dto.response.TeamsResponseDto;
import com.dhoon.transfertracker.internal.team.exception.InvalidLeagueCodeValueException;
import com.dhoon.transfertracker.internal.team.exception.InvalidTeamIdException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class TeamQueryOrchestrationService {

    private final TeamQueryTxService txService;

    public TeamItemResponseDto getTeam(Long teamId) {
        validateTeamId(teamId);

        return txService.getTeam(teamId);
    }

    public TeamsResponseDto getTeams() {
        return txService.getTeams();
    }

    public TeamsResponseDto getLeagueTeams(LeagueCode leagueCode) {
        validateLeagueCode(leagueCode);
        return txService.getLeagueTeams(leagueCode);
    }

    public TeamPageInfosResponseDto getTeamInfo(Long teamId) {
        validateTeamId(teamId);
        return txService.getTeamInfo(teamId);
    }


    private void validateTeamId(Long teamId) {
        if (teamId == null) {
            throw new InvalidTeamIdException();
        }
    }

    private void validateLeagueCode(LeagueCode leagueCode) {
        if (leagueCode == null) {
            throw new InvalidLeagueCodeValueException();
        }
    }

}
