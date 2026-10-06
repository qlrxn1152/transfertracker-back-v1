package com.dhoon.transfertracker.external.football.service;

import com.dhoon.transfertracker.external.football.dto.PlayerSaveResponseDto;
import com.dhoon.transfertracker.external.football.dto.TeamPlayerSaveResponseDto;
import com.dhoon.transfertracker.external.football.dto.TeamSaveResponseDto;
import com.dhoon.transfertracker.external.football.dto.TransferSaveResponseDto;
import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.dto.response.TeamItemResponseDto;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.teamplayer.domain.TeamPlayer;
import com.dhoon.transfertracker.internal.teamplayer.repository.TeamPlayerRepository;
import com.dhoon.transfertracker.internal.transfer.domain.Transfer;
import com.dhoon.transfertracker.internal.transfer.dto.response.PlayerTransferItemResponseDto;
import com.dhoon.transfertracker.internal.transfer.dto.response.PlayerTransfersResponseDto;
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


    public TeamSaveResponseDto getOrCreateTeamResponse(JsonNode teamData) {
        Team team = getOrCreateTeam(teamData);
        return TeamSaveResponseDto.of(team);
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

    public TeamPlayerSaveResponseDto getOrCreateTeamPlayerResponse(JsonNode playerData, JsonNode teamData) {
        Player player = getOrCreatePlayer(playerData);
        Team team = getOrCreateTeam(teamData);
        TeamPlayer teamPlayer = getOrCreateTeamPlayer(player, team);

        return TeamPlayerSaveResponseDto.of(teamPlayer);
    }

    private TeamPlayer getOrCreateTeamPlayer(Player player, Team team) {
        return teamPlayerRepository.findByPlayerIdAndTeamId(player.getId(), team.getId())
                .orElseGet(() -> teamPlayerRepository.save(
                        TeamPlayer.of(
                                team,
                                player
                        )
                ));
    }

    public PlayerTransfersResponseDto getOrCreatePlayerTransfersResponse(JsonNode playerData, JsonNode transferDatas) {
        Player player = getOrCreatePlayer(playerData);
        List<PlayerTransferItemResponseDto> transfers = getOrCreatePlayerTransfers(player, transferDatas);

        return PlayerTransfersResponseDto.of(player.getDisplayName(), transfers);
    }

    private List<PlayerTransferItemResponseDto> getOrCreatePlayerTransfers(Player player, JsonNode transferDatas) {
        List<PlayerTransferItemResponseDto> playerTransfers = new ArrayList<>();

        transferDatas.forEach(transferData -> {

            JsonNode teamData = transferData.get("teams");

            String transferType = transferData.get("type").asString();
            LocalDate transferDate = LocalDate.parse(transferData.get("date").asString());

            Team inTeam = getOrCreateTeam(teamData.get("in"));
            Team outTeam = getOrCreateTeam(teamData.get("out"));

            Transfer transfer = transferRepository.findByInTeamIdAndOutTeamIdAndPlayerIdAndTransferDate(inTeam.getId(), outTeam.getId(), player.getId(), transferDate)
                    .orElseGet(() -> transferRepository.save(Transfer.of(
                            player, inTeam, outTeam, transferType, transferDate
                    )));

            playerTransfers.add(PlayerTransferItemResponseDto.of(transfer));
        });

        return playerTransfers;
    }

    // 실행하지 않기를 권장합니다. ( 2024 년 기준 데이터이므로, 새로만든 리그가 아니라, 기존에 있던 리그면 팀 데이터가 왜곡됨.)
    public TeamItemResponseDto getOrCreateTeamAndAssignLeagueResponse(JsonNode teamData, LeagueCode leagueCode) {
        TeamItemResponseDto responseDto = getOrCreateTeamAndAssignLeague(teamData, leagueCode);

        return responseDto;
    }

    private TeamItemResponseDto getOrCreateTeamAndAssignLeague(JsonNode teamData, LeagueCode leagueCode) {
        Team team = getOrCreateTeam(teamData);
        team.assignTeamLeague(leagueCode);

        return TeamItemResponseDto.of(team);
    }

    public String getDisplayTeamName(Long teamApiId) {
        Team team = teamRepository.findByApiFootballId(teamApiId)
                .orElseThrow();

        return team.getDisplayName();
    }

}
