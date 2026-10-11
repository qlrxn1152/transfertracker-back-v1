package com.dhoon.transfertracker.internal.player.service.impl;

import com.dhoon.transfertracker.internal.player.dto.response.PlayerItemResponseDto;
import com.dhoon.transfertracker.internal.player.dto.response.PlayersResponseDto;
import com.dhoon.transfertracker.internal.player.exception.InvalidPlayerIdException;
import com.dhoon.transfertracker.internal.player.exception.InvalidPlayerSearchPageValueException;
import com.dhoon.transfertracker.internal.player.service.PlayerService;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PlayerOrchestrationService {

    private final PlayerService playerTxService;

    public PlayerItemResponseDto getPlayer(Long playerId) {
        validatePlayerIdIsNotNull(playerId);
        return playerTxService.getPlayerResponse(playerId);
    }



    public PlayersResponseDto getPlayers(int page, String keyWord, LeagueCode leagueCode, Long teamId) {
        validatePageValue(page);

        PageRequest pageable = PageRequest.of(
                page,
                50,
                Sort.by(Sort.Order.asc("player.playerName"), Sort.Order.asc("player.id"))
        );

        String normalizedKeyWord = getNormalizedKeyWord(keyWord);

        return playerTxService.getPlayers(pageable, normalizedKeyWord, leagueCode, teamId);
    }




    private String getNormalizedKeyWord(String keyWord) {
        return keyWord == null ? "" : keyWord.trim();
    }

    private void validatePlayerIdIsNotNull(Long playerId) {
        if (playerId == null) {
            throw new InvalidPlayerIdException();
        }
    }

    private void validatePageValue(int page) {
        if (page < 0) {
            throw new InvalidPlayerSearchPageValueException();
        }
    }


}