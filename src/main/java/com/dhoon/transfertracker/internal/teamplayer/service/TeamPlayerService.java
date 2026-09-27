package com.dhoon.transfertracker.internal.teamplayer.service;

import com.dhoon.transfertracker.internal.teamplayer.dto.response.TeamPlayersResponseDto;

public interface TeamPlayerService {

    TeamPlayersResponseDto getTeamPlayers(Long teamId);
}
