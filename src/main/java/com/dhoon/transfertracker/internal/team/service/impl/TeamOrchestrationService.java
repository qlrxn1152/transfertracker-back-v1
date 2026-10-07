package com.dhoon.transfertracker.internal.team.service.impl;

import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.dto.response.TeamItemResponseDto;
import com.dhoon.transfertracker.internal.team.dto.response.TeamPageInfoForTeamResponseDto;
import com.dhoon.transfertracker.internal.team.dto.response.TeamPageInfosResponseDto;
import com.dhoon.transfertracker.internal.team.dto.response.TeamsResponseDto;
import com.dhoon.transfertracker.internal.team.exception.InvalidLeagueCodeValueException;
import com.dhoon.transfertracker.internal.team.exception.NotFoundTeamException;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.team.service.TeamService;
import com.dhoon.transfertracker.internal.teamplayer.dto.response.TeamPlayerItemResponseDto;
import com.dhoon.transfertracker.internal.teamplayer.dto.response.TeamPlayersResponseDto;
import com.dhoon.transfertracker.internal.teamplayer.repository.TeamPlayerRepository;
import com.dhoon.transfertracker.internal.transfer.dto.response.TransferPostsResponseDto;
import com.dhoon.transfertracker.internal.transfer.exception.InvalidTeamIdException;
import com.dhoon.transfertracker.internal.transferpost.dto.response.TransferPostItemResponseDto;
import com.dhoon.transfertracker.internal.transferpost.repository.TransferPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class TeamOrchestrationService {

    private final TeamService teamTxService;

    public TeamItemResponseDto getTeam(Long teamId) {
        checkTeamIdIsNotNull(teamId);

        return teamTxService.getTeam(teamId);
    }

    public TeamsResponseDto getTeams() {
        return teamTxService.getTeams();
    }

    public TeamsResponseDto getLeagueTeams(LeagueCode leagueCode) {
        checkLeagueCodeIsInvalid(leagueCode);
        return teamTxService.getLeagueTeams(leagueCode);
    }

    public TeamPageInfosResponseDto getTeamInfo(Long teamId) {
        checkTeamIdIsNotNull(teamId);
        return teamTxService.getTeamInfo(teamId);
    }






    public void checkTeamIdIsNotNull(Long teamId) {
        if (teamId == null) {
            throw new InvalidTeamIdException();
        }
    }

    public void checkLeagueCodeIsInvalid(LeagueCode leagueCode) {
        if (leagueCode == null) {
            throw new InvalidLeagueCodeValueException();
        }
    }



}
