package com.dhoon.transfertracker.external.football.client;

import com.dhoon.transfertracker.external.football.dto.PlayerSaveResponseDto;
import com.dhoon.transfertracker.external.football.dto.TeamSaveResponseDto;
import com.dhoon.transfertracker.external.football.dto.player.PlayerItemResponseDto;
import com.dhoon.transfertracker.external.football.dto.player.PlayersResponseDto;
import com.dhoon.transfertracker.external.football.dto.playertransfer.PlayerTransferInfoResponseDto;
import com.dhoon.transfertracker.external.football.dto.playertransfer.PlayerTransferResponseDto;
import com.dhoon.transfertracker.external.football.dto.team.TeamInfoResponseDto;
import com.dhoon.transfertracker.internal.domain.Player;
import com.dhoon.transfertracker.internal.domain.Team;
import com.dhoon.transfertracker.internal.domain.Transfer;
import com.dhoon.transfertracker.internal.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.repository.TeamRepository;
import com.dhoon.transfertracker.internal.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiFootballClient {

    private final RestClient restClient;

    private final TransferRepository transferRepository;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;

    /**
     * 외부 API 를 호출해서, 해당 선수 데이터를 DB 에 저장하는 작업.
     * @param playerId -> 외부 API ID
     */
    public PlayerSaveResponseDto savePlayer(Long playerId) {
        JsonNode node = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/players")
                        .path("/profiles")
                        .queryParam("player", playerId)
                        .build()
                )
                .retrieve()
                .body(JsonNode.class);

        JsonNode playerData = node.get("response").get(0).get("player");

        Player player = Player.of(playerData.get("name").asString(), playerData.get("id").asLong());
        playerRepository.save(player);

        return PlayerSaveResponseDto.of(player);
    }


    /**
     * 외부 API 를 호출해서, 해당 팀 데이터를 DB 에 저장하는 작업.
     * @param teamId -> 외부 API ID
     */
    public TeamSaveResponseDto saveTeam(Long teamId) {
        JsonNode node = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/teams")
                        .queryParam("id", teamId)
                        .build()
                )
                .retrieve()
                .body(JsonNode.class);

        JsonNode teamData = node.get("response").get(0).get("team");

        Team team = Team.of(teamData.get("name").asString(), teamData.get("id").asLong());
        teamRepository.save(team);

        return TeamSaveResponseDto.of(team);
    }







}
