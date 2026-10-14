package com.dhoon.transfertracker.internal.player.sync.service.impl;


import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.player.sync.dto.response.PlayerSaveResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class PlayerSyncTxService {

    private final PlayerRepository playerRepository;

    public PlayerSaveResponseDto getOrCreatePlayerResponse(JsonNode playerData) {
        Player player = getOrCreatePlayer(playerData);

        return PlayerSaveResponseDto.of(player);
    }

    private Player getOrCreatePlayer(JsonNode playerData) {
        long playerApiId = playerData.get("id").asLong();

        return playerRepository.findByApiFootballId(playerApiId)
                .orElseGet(() -> playerRepository.save(
                        Player.of(
                                playerData.get("name").asString(),
                                playerData.get("id").asLong()
                        ))
                );
    }

}
