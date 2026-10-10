package com.dhoon.transfertracker.internal.team.service;

import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.dto.response.TeamItemResponseDto;
import com.dhoon.transfertracker.internal.team.dto.response.TeamPageInfosResponseDto;
import com.dhoon.transfertracker.internal.team.dto.response.TeamsResponseDto;

public interface TeamService {

    TeamItemResponseDto getTeam(Long teamId);

    TeamsResponseDto getTeams();

    TeamsResponseDto getLeagueTeams(LeagueCode leagueCode);

    TeamPageInfosResponseDto getTeamInfo(Long teamId);
}
