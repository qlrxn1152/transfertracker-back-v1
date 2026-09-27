package com.dhoon.transfertracker.internal.player.service.impl;

import com.dhoon.transfertracker.internal.player.dto.response.PlayerItemResponseDto;
import com.dhoon.transfertracker.internal.player.dto.response.PlayersResponseDto;
import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.player.service.PlayerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
class PlayerServiceImpl implements PlayerService {

    private final PlayerRepository playerRepository;

    @Override
    @Transactional(readOnly = true)
    public PlayerItemResponseDto getPlayer(Long playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow();

        return PlayerItemResponseDto.of(player);
    }

    @Override
    @Transactional(readOnly = true)
    public PlayersResponseDto getPlayers() {
        List<PlayerItemResponseDto> players = playerRepository.findAll()
                .stream()
                .map(PlayerItemResponseDto::of)
                .toList();

        return PlayersResponseDto.of(players);
    }

}