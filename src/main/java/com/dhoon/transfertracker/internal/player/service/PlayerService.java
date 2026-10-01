package com.dhoon.transfertracker.internal.player.service;

import com.dhoon.transfertracker.internal.player.dto.response.PlayerItemResponseDto;
import com.dhoon.transfertracker.internal.player.dto.response.PlayersResponseDto;

public interface PlayerService {

    PlayerItemResponseDto getPlayer(Long playerId);

    PlayersResponseDto getPlayers(int page, String keyWord);


}
