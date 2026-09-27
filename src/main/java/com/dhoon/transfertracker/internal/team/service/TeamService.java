package com.dhoon.transfertracker.internal.team.service;

import com.dhoon.transfertracker.internal.team.dto.response.TeamItemResponseDto;
import com.dhoon.transfertracker.internal.team.dto.response.TeamsResponseDto;

public interface TeamService {

    TeamItemResponseDto getTeam(Long teamId);

    TeamsResponseDto getTeams();

    TeamsResponseDto getLeagueTeams(String leagueCode);
}
