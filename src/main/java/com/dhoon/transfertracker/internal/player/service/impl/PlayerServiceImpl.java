package com.dhoon.transfertracker.internal.player.service.impl;

import com.dhoon.transfertracker.internal.player.dto.response.PlayerItemResponseDto;
import com.dhoon.transfertracker.internal.player.dto.response.PlayersResponseDto;
import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.exception.InvalidPlayerSearchPageValueException;
import com.dhoon.transfertracker.internal.player.exception.NotFoundPlayerException;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.player.service.PlayerService;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.teamplayer.domain.TeamPlayer;
import com.dhoon.transfertracker.internal.teamplayer.repository.TeamPlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
class PlayerServiceImpl implements PlayerService {

    private final PlayerRepository playerRepository;
    private final TeamPlayerRepository teamPlayerRepository;


    // N + 1
    @Override
    @Transactional(readOnly = true)
    public PlayerItemResponseDto getPlayer(Long playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(NotFoundPlayerException::new);

        return teamPlayerRepository.findByPlayerId(playerId)
                .map(tp -> PlayerItemResponseDto.of(player, tp.getTeam()))
                .orElseGet(() -> PlayerItemResponseDto.of(player));
    }


    @Override
    @Transactional(readOnly = true)
    public PlayersResponseDto getPlayers(int page, String keyWord, LeagueCode leagueCode, Long teamId) {
        if (page < 0) {
            throw new InvalidPlayerSearchPageValueException();
        }

        PageRequest pageable = PageRequest.of(
                page,
                50,
                Sort.by(Sort.Order.asc("player.playerName"), Sort.Order.asc("player.id"))
        );

        String normalizedKeyWord = keyWord == null ? "" : keyWord.trim();
        Slice<TeamPlayer> playerSlice;

        playerSlice = teamPlayerRepository.findTeamPlayers(normalizedKeyWord, pageable, leagueCode, teamId);


        List<PlayerItemResponseDto> data = playerSlice.getContent()
                .stream()
                .map(PlayerItemResponseDto::of)
                .toList();

        return PlayersResponseDto.of(data, playerSlice.hasNext(), playerSlice.hasPrevious());
    }

}