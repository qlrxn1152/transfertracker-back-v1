package com.dhoon.transfertracker.internal.service;

import com.dhoon.transfertracker.external.football.dto.team.TeamInfoResponseDto;
import com.dhoon.transfertracker.internal.dto.team.TeamItemResponseDto;
import com.dhoon.transfertracker.internal.dto.team.TeamsResponseDto;

public interface TeamService {

    TeamItemResponseDto getTeam(Long teamId);

    TeamsResponseDto getTeams();

    TeamsResponseDto getLeagueTeams(String leagueCode);
}
