package com.dhoon.transfertracker.external.football.service;

import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
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


    public Player getOrCreatePlayerTransfers(JsonNode playerData, JsonNode transferData) {

        Player player = getOrCreatePlayer(playerData);

        transferData.forEach(transfer -> {
            LocalDate transferDate = LocalDate.parse(transfer.get("date").asString());
            String transferType = transfer.get("type").asString();

            JsonNode inTeamData = transfer.get("teams").get("in");
            JsonNode outTeamData = transfer.get("teams").get("out");

            Team inTeam = getOrCreateTeam(inTeamData);
            Team outTeam = getOrCreateTeam(outTeamData);

            transferRepository.findByInTeamIdAndOutTeamIdAndPlayerIdAndTransferDate(inTeam.getId(), outTeam.getId(), player.getId(), transferDate)
                    .orElseGet(() -> transferRepository.save(Transfer.of(
                            player, inTeam, outTeam, transferType, transferDate
                    )));
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

    // 실행하지 않기를 권장합니다. ( 2024 년 기준 데이터이므로, 새로만든 리그가 아니라, 기존에 있던 리그면 팀 데이터가 왜곡됨.)
    public void getOrCreateTeamAndAssignLeague(JsonNode teamData, LeagueCode leagueCode) {
        getOrCreateTeam(teamData).assignTeamLeague(leagueCode);
    }

}
