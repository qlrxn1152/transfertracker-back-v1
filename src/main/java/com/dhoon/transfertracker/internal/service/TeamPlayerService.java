package com.dhoon.transfertracker.internal.service;

import com.dhoon.transfertracker.internal.dto.response.TeamPlayersResponseDto;

public interface TeamPlayerService {

    TeamPlayersResponseDto getTeamPlayers(Long teamId);
}
