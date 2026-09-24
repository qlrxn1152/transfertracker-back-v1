package com.dhoon.transfertracker.internal.service;

import com.dhoon.transfertracker.internal.dto.response.PlayerItemResponseDto;
import com.dhoon.transfertracker.internal.dto.response.PlayersResponseDto;

public interface PlayerService {

    PlayerItemResponseDto getPlayer(Long playerId);

    PlayersResponseDto getPlayers();
}
