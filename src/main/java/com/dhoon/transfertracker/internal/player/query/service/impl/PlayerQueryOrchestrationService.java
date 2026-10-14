package com.dhoon.transfertracker.internal.player.query.service.impl;

import com.dhoon.transfertracker.internal.player.query.dto.response.PlayerItemResponseDto;
import com.dhoon.transfertracker.internal.player.query.dto.response.PlayersResponseDto;
import com.dhoon.transfertracker.internal.player.exception.InvalidPlayerIdException;
import com.dhoon.transfertracker.internal.player.exception.InvalidPlayerSearchPageValueException;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class PlayerQueryOrchestrationService {

    private final PlayerQueryTxService txService;

    public PlayerItemResponseDto getPlayer(Long playerId) {
        validatePlayerIdIsNotNull(playerId);
        return txService.getPlayerResponse(playerId);
    }



    public PlayersResponseDto getPlayers(int page, String keyWord, LeagueCode leagueCode, Long teamId) {
        validatePageValue(page);

        PageRequest pageable = PageRequest.of(
                page,
                50,
                Sort.by(Sort.Order.asc("player.playerName"), Sort.Order.asc("player.id"))
        );

        String normalizedKeyWord = getNormalizedKeyWord(keyWord);

        return txService.getPlayers(pageable, normalizedKeyWord, leagueCode, teamId);
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
