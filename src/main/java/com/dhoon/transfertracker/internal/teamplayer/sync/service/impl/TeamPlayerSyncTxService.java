package com.dhoon.transfertracker.internal.teamplayer.sync.service.impl;

import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.teamplayer.domain.TeamPlayer;
import com.dhoon.transfertracker.internal.teamplayer.repository.TeamPlayerRepository;
import com.dhoon.transfertracker.internal.teamplayer.sync.dto.response.TeamPlayerSaveResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class TeamPlayerSyncTxService {

    private final TeamPlayerRepository teamPlayerRepository;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;

    public TeamPlayerSaveResponseDto getOrCreateTeamPlayerResponse(JsonNode playerData, JsonNode teamData) {
        Player player = getOrCreatePlayer(playerData);
        Team team = getOrCreateTeam(teamData);
        TeamPlayer teamPlayer = getOrCreateTeamPlayer(player, team);

        return TeamPlayerSaveResponseDto.of(teamPlayer);
    }












    // ========================================= //

    private TeamPlayer getOrCreateTeamPlayer(Player player, Team team) {
        return teamPlayerRepository.findByPlayerIdAndTeamId(player.getId(), team.getId())
                .orElseGet(() -> teamPlayerRepository.save(
                        TeamPlayer.of(
                                team,
                                player
                        )
                ));
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

    private Team getOrCreateTeam(JsonNode teamData) {
        long teamApiId = teamData.get("id").asLong();

        return teamRepository.findByApiFootballId(teamApiId)
                .orElseGet(() -> teamRepository.save(
                        Team.of(
                                teamData.get("name").asString(),
                                teamData.get("id").asLong()
                        )
                ));
    }

}
