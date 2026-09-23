package com.dhoon.transfertracker.internal.service;

import com.dhoon.transfertracker.external.football.dto.team.TeamInfoResponseDto;

public interface TeamService {

    TeamInfoResponseDto getTeam(Long teamId);
}
