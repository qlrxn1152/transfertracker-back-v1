package com.dhoon.transfertracker.external.football.service;

import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.teamplayer.domain.TeamPlayer;
import com.dhoon.transfertracker.internal.teamplayer.repository.TeamPlayerRepository;
import com.dhoon.transfertracker.internal.transfer.domain.Transfer;
import com.dhoon.transfertracker.internal.transfer.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class FootballSyncTxService {

    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final TransferRepository transferRepository;
    private final TeamPlayerRepository teamPlayerRepository;


    public Player getOrCreatePlayer(JsonNode playerData) {
        long playerApiId = playerData.get("id").asLong();

        return playerRepository.findByApiFootballId(playerApiId)
                .orElseGet(() -> playerRepository.save(
                        Player.of(
                                playerData.get("name").asString(),
                                playerData.get("id").asLong()
                        ))
                );
    }

    // Transaction
    public Team getOrCreateTeam(JsonNode teamData) {
        long teamApiId = teamData.get("id").asLong();

        return teamRepository.findByApiFootballId(teamApiId)
                .orElseGet(() -> teamRepository.save(
                        Team.of(
                                teamData.get("name").asString(),
                                teamData.get("id").asLong()
                        )
                ));
    }


    public Player getOrCreatePlayerTransfers(JsonNode playerData, JsonNode transferData, Long playerApiId) {
        Player player = getOrCreatePlayer(playerData);// 플레이어 없으면 먼저 저장

        // 팀 없으면 먼저 저장
        transferData.forEach(transfer -> {

            LocalDate transferDate = LocalDate.parse(transferData.get("date").asString());
            String transferType = transferData.get("type").asString();
            JsonNode inTeamData = transfer.get("teams").get("in");
            JsonNode outTeamData = transfer.get("teams").get("out");

            long inTeamId = inTeamData.get("id").asLong();
            long outTeamId = outTeamData.get("id").asLong();

            Team inTeam = getOrCreateTeam(inTeamData);
            Team outTeam = getOrCreateTeam(outTeamData);

            transferRepository.findByInTeamIdAndOutTeamIdAndPlayerIdAndTransferDate(inTeamId, outTeamId, playerApiId, LocalDate.now())
                    .orElseGet(() -> transferRepository.save(Transfer.of(player, inTeam, outTeam, transferType, transferDate)));
        });

        return player;
    }



    public TeamPlayer getOrCreateTeamPlayer(Player player, Team team) {
        return teamPlayerRepository.findByPlayerIdAndTeamId(player.getId(), team.getId())
                .orElseGet(() -> teamPlayerRepository.save(
                        TeamPlayer.of(
                                team,
                                player
                        )
                ));
    }

}
