package com.dhoon.transfertracker.internal.player.service.impl;

import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.dto.response.PlayerItemResponseDto;
import com.dhoon.transfertracker.internal.player.dto.response.PlayersResponseDto;
import com.dhoon.transfertracker.internal.player.exception.NotFoundPlayerException;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.player.service.PlayerService;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.teamplayer.domain.TeamPlayer;
import com.dhoon.transfertracker.internal.teamplayer.repository.TeamPlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Transactional
@Service
public class PlayerTxService implements PlayerService {

    private final PlayerRepository playerRepository;
    private final TeamPlayerRepository teamPlayerRepository;

    @Override
    @Transactional(readOnly = true)
    public PlayerItemResponseDto getPlayerResponse(Long playerId) {
        return teamPlayerRepository.findByPlayerId(playerId)
                .map(PlayerItemResponseDto::of)
                .orElseGet(() -> {
                    Player player = playerRepository.findById(playerId)
                            .orElseThrow(NotFoundPlayerException::new);

                    return PlayerItemResponseDto.of(player);
                });
    }



    @Override
    @Transactional(readOnly = true)
    public PlayersResponseDto getPlayers(Pageable pageable, String keyWord, LeagueCode leagueCode, Long teamId) {

        Slice<TeamPlayer> playerSlice = teamPlayerRepository.findTeamPlayers(keyWord, pageable, leagueCode, teamId);

        List<PlayerItemResponseDto> players = playerSlice.getContent()
                .stream()
                .map(PlayerItemResponseDto::of)
                .toList();

        return PlayersResponseDto.of(players, playerSlice.hasNext(), playerSlice.hasPrevious());
    }
}
