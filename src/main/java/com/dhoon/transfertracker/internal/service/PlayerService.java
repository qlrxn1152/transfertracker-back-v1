package com.dhoon.transfertracker.internal.service;

import com.dhoon.transfertracker.external.football.dto.player.PlayersResponseDto;
import com.dhoon.transfertracker.internal.dto.response.PlayerItemResponseDto;

public interface PlayerService {


    PlayerItemResponseDto getPlayer(Long playerId);
}
