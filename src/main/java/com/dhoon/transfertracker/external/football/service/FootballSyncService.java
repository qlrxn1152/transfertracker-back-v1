package com.dhoon.transfertracker.external.football.service;

import com.dhoon.transfertracker.external.football.client.ApiFootballHttpClient;
import com.dhoon.transfertracker.external.football.dto.*;
import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.teamplayer.domain.TeamPlayer;
import com.dhoon.transfertracker.internal.teamplayer.repository.TeamPlayerRepository;
import com.dhoon.transfertracker.internal.transfer.domain.Transfer;
import com.dhoon.transfertracker.internal.transfer.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class FootballSyncService {

    private final ApiFootballHttpClient footballRestClient;
    private final FootballSyncTxService footballSyncTxService;


    /**
     * 외부 API 를 호출해서, 해당 선수 데이터를 DB 에 저장하는 작업.
     * @param playerApiId -> 외부 API ID
     */
    public PlayerSaveResponseDto syncPlayer(Long playerApiId) {
        JsonNode playerData = footballRestClient.callExternalPlayerApi(playerApiId);
        Player player = footballSyncTxService.getOrCreatePlayer(playerData);

        return PlayerSaveResponseDto.of(player);
    }

    /**
     * 외부 API 를 호출해서, 해당 팀 데이터를 DB 에 저장하는 작업.
     * @param teamApiId -> 외부 API ID
     */
    public TeamSaveResponseDto syncTeam(Long teamApiId) {
        JsonNode teamData = footballRestClient.callExternalTeamApi(teamApiId);
        Team team = footballSyncTxService.getOrCreateTeam(teamData);

        return TeamSaveResponseDto.of(team);
    }

    /**
     * 외부 API 를 호출해서, 해당 선수의 이적 데이터를 DB 에 저장하는 작업.
     * @param playerApiId -> 외부 API ID
     */
    public TransferSaveResponseDto syncPlayerTransfers(Long playerApiId) {
        JsonNode node = footballRestClient.callExternalPlayerTransferApi(playerApiId);

        JsonNode playerData = node.get("response").get(0).get("player");
        JsonNode transferData = node.get("response").get(0).get("transfers");

        Player player = footballSyncTxService.getOrCreatePlayerTransfers(playerData, transferData, playerApiId);

        return TransferSaveResponseDto.of(player);
    }

    /**
     * 외부 API 를 호출해서, 해당 팀의 선수들을 DB 에 저장하는 작업.
     * @param teamApiId -> 외부 API ID
     */
    public TeamPlayersSaveResponseDto syncTeamPlayers(Long teamApiId) {
        JsonNode node = footballRestClient.callExternalTeamPlayersApi(teamApiId);
        JsonNode teamData = node.get("response").get(0).get("team");
        JsonNode playersData = node.get("response").get(0).get("players");

        Team saveTeam = footballSyncTxService.getOrCreateTeam(teamData);

        List<TeamPlayerSaveResponseDto> response = new ArrayList<>();


        playersData.forEach(player -> {
            Player savePlayer = footballSyncTxService.getOrCreatePlayer(player);
            TeamPlayer saveTeamPlayer = footballSyncTxService.getOrCreateTeamPlayer(savePlayer, saveTeam);

            response.add(TeamPlayerSaveResponseDto.of(saveTeamPlayer));
        });

        return TeamPlayersSaveResponseDto.of(response);
    }

    /**
     * 외부 API 를 호출해서, 해당 팀의 선수들을 DB 에 저장하는 작업. ( 2020 년 이상의 이적정보만 저장합니다.)
     * @param teamApiId -> 외부 API ID
     */
    public String saveTeamTransfers(Long teamApiId) {
        JsonNode node = footballRestClient.callExternalTeamTransfersApi(teamApiId);

        node.get("response")
                .forEach(data -> {
                    JsonNode playerData = data.get("player");
                    Player player = footballSyncTxService.getOrCreatePlayer(playerData);

                    data.get("transfers").forEach(transferData -> footballSyncTxService.getOrCreatePlayerTransfers(playerData, transferData, player.getApiFootballId()));
                });

        return "FootballSyncService.saveTeamTransfers";
    }

    /**
     * 외부 API를 호출해서, 해당 리그에 해당 시즌에 속했던 팀들을 가지고 오고, 해당 팀들을 리그에 배치합니다.
     * @param leagueCode -> ENUM
     */
    public String syncLeagueTeams(LeagueCode leagueCode) {
        JsonNode node = footballRestClient.callExternalLeagueTeamsApi(leagueCode);

        node.get("response")
                .forEach(teamData -> footballSyncTxService.getOrCreateTeam(teamData).assignTeamLeague(leagueCode));


        return "FootballSyncService.syncLeagueTeams";
    }

}
