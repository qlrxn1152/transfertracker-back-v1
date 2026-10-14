package com.dhoon.transfertracker.external.football.service;

import com.dhoon.transfertracker.external.football.dto.PlayerSaveResponseDto;
import com.dhoon.transfertracker.external.football.dto.TeamPlayerSaveResponseDto;
import com.dhoon.transfertracker.external.football.dto.TeamSaveResponseDto;
import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.query.dto.response.TeamItemResponseDto;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.teamplayer.domain.TeamPlayer;
import com.dhoon.transfertracker.internal.teamplayer.repository.TeamPlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class FootballSyncTxService {

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
