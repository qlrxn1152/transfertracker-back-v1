package com.dhoon.transfertracker.internal.player.service;

import com.dhoon.transfertracker.internal.player.dto.response.PlayerItemResponseDto;
import com.dhoon.transfertracker.internal.player.dto.response.PlayersResponseDto;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import org.springframework.data.domain.Pageable;

public interface PlayerService {

    PlayerItemResponseDto getPlayerResponse(Long playerId);

    PlayersResponseDto getPlayers(Pageable pageable, String keyWord, LeagueCode leagueCode, Long teamId);


}
