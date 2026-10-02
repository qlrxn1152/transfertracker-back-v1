package com.dhoon.transfertracker.internal.player.service.impl;

import com.dhoon.transfertracker.internal.player.dto.response.PlayerItemResponseDto;
import com.dhoon.transfertracker.internal.player.dto.response.PlayersResponseDto;
import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.exception.InvalidPlayerSearchPageValueException;
import com.dhoon.transfertracker.internal.player.exception.NotFoundPlayerException;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.player.service.PlayerService;
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

    @Override
    @Transactional(readOnly = true)
    public PlayerItemResponseDto getPlayer(Long playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(NotFoundPlayerException::new);

        if (!teamPlayerRepository.existsByPlayerId(playerId)) {
            PlayerItemResponseDto.of(player);
        }

        TeamPlayer teamPlayer = teamPlayerRepository.findByPlayerId(playerId).get();

        return PlayerItemResponseDto.of(player, teamPlayer.getTeam());
    }

    @Override
    @Transactional(readOnly = true)
    public PlayersResponseDto getPlayers(int page, String keyWord) {
        if (page < 0) {
            throw new InvalidPlayerSearchPageValueException();
        }

        PageRequest pageable = PageRequest.of(
                page,
                50,
                Sort.by(Sort.Order.asc("playerName"), Sort.Order.asc("id"))
        );

        String normalizedKeyWord = keyWord == null ? "" : keyWord.trim();
        Slice<Player> playerSlice;

        if (normalizedKeyWord.isBlank()) {
            playerSlice = playerRepository.findAllBy(pageable);
        }

        else {
            playerSlice = playerRepository.findByPlayerNameContainingIgnoreCase(normalizedKeyWord, pageable);
        }

        List<PlayerItemResponseDto> players = new ArrayList<>();

        for (Player player : playerSlice.getContent()) {
            if (teamPlayerRepository.existsByPlayerId(player.getId())) {
                TeamPlayer teamPlayer = teamPlayerRepository.findByPlayerId(player.getId()).get();

                players.add(PlayerItemResponseDto.of(player, teamPlayer.getTeam()));
            }
            else {
                players.add(PlayerItemResponseDto.of(player));
            }
        }


        return PlayersResponseDto.of(players, playerSlice.hasNext(), playerSlice.hasPrevious());
    }

}