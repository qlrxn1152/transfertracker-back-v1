package com.dhoon.transfertracker.internal.service.impl;

import com.dhoon.transfertracker.external.football.client.ApiFootballClient;
import com.dhoon.transfertracker.internal.domain.Player;
import com.dhoon.transfertracker.internal.dto.response.PlayerItemResponseDto;
import com.dhoon.transfertracker.internal.dto.response.PlayersResponseDto;
import com.dhoon.transfertracker.internal.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.service.PlayerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class PlayerServiceImpl implements PlayerService {

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
