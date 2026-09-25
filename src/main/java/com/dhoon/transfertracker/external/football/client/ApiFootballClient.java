package com.dhoon.transfertracker.external.football.client;

import com.dhoon.transfertracker.TeamTransferData;
import com.dhoon.transfertracker.external.football.dto.PlayerSaveResponseDto;
import com.dhoon.transfertracker.external.football.dto.TeamPlayersSaveResponseDto;
import com.dhoon.transfertracker.external.football.dto.TeamSaveResponseDto;
import com.dhoon.transfertracker.external.football.dto.TransferSaveResponseDto;
import com.dhoon.transfertracker.external.football.dto.TeamPlayerSaveResponseDto;
import com.dhoon.transfertracker.internal.domain.Player;
import com.dhoon.transfertracker.internal.domain.Team;
import com.dhoon.transfertracker.internal.domain.Transfer;
import com.dhoon.transfertracker.internal.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.repository.TeamRepository;
import com.dhoon.transfertracker.internal.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
@Transactional
public class ApiFootballClient {

    private final RestClient restClient;

    private final TransferRepository transferRepository;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;

    /**
     * 외부 API 를 호출해서, 해당 선수 데이터를 DB 에 저장하는 작업.
     * @param playerApiId -> 외부 API ID
     */
    public PlayerSaveResponseDto syncPlayer(Long playerApiId) {
        Player player = callExternalAndPlayerSaveToDb(playerApiId);

        return PlayerSaveResponseDto.of(player);
    }

    /**
     * 외부 API 를 호출해서, 해당 팀 데이터를 DB 에 저장하는 작업.
     * @param teamApiID -> 외부 API ID
     */
    public TeamSaveResponseDto syncTeam(Long teamApiID) {
        Team team = callExternalAndTeamSaveToDb(teamApiID);

        return TeamSaveResponseDto.of(team);
    }

    /**
     * 외부 API 를 호출해서, 해당 선수의 이적 데이터를 DB 에 저장하는 작업.
     * @param playerApiId -> 외부 API ID
     */
    public TransferSaveResponseDto syncPlayerTransfers(Long playerApiId) {
        Player player = playerRepository.findByApiFootballId(playerApiId).orElseThrow();

        JsonNode node = callExternalPlayerTransferApi(playerApiId);
        JsonNode transfers = getPlayerTransfers(node);

        transfers.forEach(
                transfer -> {
                    TeamTransferData data = TeamTransferData.of(transfer);

                    Team inTeam = getOrCreateTeam(data.getInTeam());
                    Team outTeam = getOrCreateTeam(data.getOutTeam());

                    savePlayerTransfers(player, data, inTeam, outTeam);
                }
        );

        return TransferSaveResponseDto.of(player);
    }





















    /**
     * 외부 API 를 호출해서, 해당 팀의 선수들을 DB 에 저장하는 작업.
     * @param teamApiId -> 외부 API ID
     */
    public TeamPlayersSaveResponseDto saveTeamPlayers(Long teamApiId) {
        JsonNode node = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/players")
                        .path("/squads")
                        .queryParam("team", teamApiId)
                        .build()
                )
                .retrieve()
                .body(JsonNode.class);

        // 팀이 존재하는지부터 확인.

        if ( !teamRepository.existsByApiFootballId(teamApiId) ) {
            syncTeam(teamApiId);
        }

        // 팀에 속한 플레이어들
        JsonNode players = node.get("response").get(0).get("players");

        List<TeamPlayerSaveResponseDto> responseDto = new ArrayList<>();

        // 팀에 속한 플레이어들중 DB 에 없는 선수들만 저장
        for (JsonNode player : players) {
            long playerApiId = player.get("id").asLong();
            String playerName = player.get("name").asString();

            if (!playerRepository.existsByApiFootballId(playerApiId)) {
                playerRepository.save(Player.of(playerName, playerApiId));

                log.info("[{}] 선수가 등록되었습니다.", playerName);
            }

            TeamPlayerSaveResponseDto playerItem = TeamPlayerSaveResponseDto.of(playerApiId, playerName);

            responseDto.add(playerItem);
        }

        return TeamPlayersSaveResponseDto.of(responseDto);
    }


    public String saveTeamTransfers(Long teamIdApiId) {
        JsonNode node = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/transfers")
                        .queryParam("team", teamIdApiId)
                        .build()
                )
                .retrieve()
                .body(JsonNode.class);


        JsonNode responses = node.get("response");

        for (JsonNode response : responses) {
            long playerApiId = response.get("player").get("id").asLong();
            String playerName = response.get("player").get("name").asString();

            if (!playerRepository.existsByApiFootballId(playerApiId)) {
                playerRepository.save(Player.of(playerName, playerApiId));

                log.info("[{}] 선수가 등록되었습니다.", playerName);
            }

            Player player = playerRepository.findByApiFootballId(playerApiId).orElseThrow();

            JsonNode transfer = response.get("transfers").get(0);

            String transferType = transfer.get("type").asString();
            LocalDate transferDate = LocalDate.parse(transfer.get("date").asString());

            long inTeamApiId = transfer.get("teams").get("in").get("id").asLong();
            String inTeamName = transfer.get("teams").get("in").get("name").asString();

            long outTeamApiId = transfer.get("teams").get("out").get("id").asLong();
            String outTeamName = transfer.get("teams").get("out").get("name").asString();

            if (!teamRepository.existsByApiFootballId(inTeamApiId)) {
                teamRepository.save(Team.of(inTeamName, inTeamApiId));
            }

            if (!teamRepository.existsByApiFootballId(outTeamApiId)) {
                teamRepository.save(Team.of(outTeamName, outTeamApiId));
            }

            Team inTeam = teamRepository.findByApiFootballId(inTeamApiId).orElseThrow();
            Team outTeam = teamRepository.findByApiFootballId(outTeamApiId).orElseThrow();

            // 2020 년 이후 데이터만 저장.
            if (transferDate.getYear() >= 2020) {
                if (transferRepository.existsByInTeamIdAndOutTeamIdAndPlayerId(inTeam.getId(), outTeam.getId(), player.getId())) {
                    log.info("이미 존재하는 이적정보입니다.");

                }
                transferRepository.save(Transfer.of(player, inTeam, outTeam, transferType, transferDate));
                // 해당 팀에 대한거이므로, 해당 팀에서 다른팀으로 이적하고 또 다시 다른팀으로 이적하면 해당 이적정보는 저장이 안되어져있음.
            }
        }

        return "OK";
    }


    // ---------------------------- SavePlayer -----------------------
    private Player callExternalAndPlayerSaveToDb(Long playerApiId) {
        JsonNode node = callExternalPlayerApi(playerApiId);
        return getOrCreatePlayer(node, playerApiId);
    }

    private @NonNull Player getOrCreatePlayer(JsonNode node, Long playerApiId) {
        JsonNode playerData = node.get("response").get(0).get("player");

        // 선수가 이미 저장되어져 있으면 새로 저장이 되는거는 아니지만, 선수를 새로넣나 넣지못하나 같은 응답 데이터를 전송해서 헷갈릴 수 있음.
        return playerRepository.findByApiFootballId(playerApiId)
                .orElseGet(() -> playerRepository.save(
                        Player.of(
                                playerData.get("name").asString(),
                                playerData.get("id").asLong()
                        ))
                );
    }

    private @Nullable JsonNode callExternalPlayerApi(Long playerApiId) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/players")
                        .path("/profiles")
                        .queryParam("player", playerApiId)
                        .build()
                )
                .retrieve()
                .body(JsonNode.class);
    }
    // ---------------------------- SavePlayer -----------------------


    // ---------------------------- SaveTeam -----------------------
    private @NonNull Team callExternalAndTeamSaveToDb(Long teamApiID) {
        JsonNode node = callExternalTeamApi(teamApiID);
        return getTeamAndSave(node);
    }

    private Team getTeamAndSave(JsonNode node) {
        JsonNode teamData = node.get("response").get(0).get("team");
        return teamRepository.save(
                Team.of(
                        teamData.get("name").asString(),
                        teamData.get("id").asLong()
                )
        );
    }

    private JsonNode callExternalTeamApi(Long teamApiID) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/teams")
                        .queryParam("id", teamApiID)
                        .build()
                )
                .retrieve()
                .body(JsonNode.class);
    }

    // ---------------------------- SaveTeam -----------------------


    // ---------------------------- SavePlayerTransfer -----------------------

    private @NonNull Team getOrCreateTeam(Team team) {
        return teamRepository.findByApiFootballId(team.getApiFootballId())
                .orElseGet(() -> teamRepository.save(team));
    }


    private void savePlayerTransfers(Player player, TeamTransferData data, Team inTeam, Team outTeam) {
        Transfer transfer = Transfer.of(player, inTeam, outTeam, data.getTransferType(), LocalDate.parse(data.getTransferDate()));

        if (!transferRepository.existsByInTeamIdAndOutTeamIdAndPlayerId(transfer.getInTeam().getId(), transfer.getOutTeam().getId(), player.getId())) {
            transferRepository.save(transfer);
            log.info("[{}] 의 이적정보가 등록되었습니다.", player.getPlayerName());
        }
    }

    private static JsonNode getPlayerTransfers(JsonNode node) {
        return node.get("response").get(0).get("transfers");
    }

    private @Nullable JsonNode callExternalPlayerTransferApi(Long playerApiId) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/transfers")
                        .queryParam("player", playerApiId)
                        .build()
                )
                .retrieve()
                .body(JsonNode.class);
    }
    // ---------------------------- SavePlayerTransfer -----------------------









}
