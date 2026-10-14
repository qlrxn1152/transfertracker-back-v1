package com.dhoon.transfertracker.internal.teamplayer.query.service;

import com.dhoon.transfertracker.internal.teamplayer.query.dto.response.TeamPlayersResponseDto;

public interface TeamPlayerService {

    TeamPlayersResponseDto getTeamPlayers(Long teamId);
}
