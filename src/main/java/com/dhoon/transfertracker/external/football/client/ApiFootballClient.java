package com.dhoon.transfertracker.external.football.client;

import com.dhoon.transfertracker.external.football.dto.PlayerSaveResponseDto;
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
     * @return
     */
    public PlayerSaveResponseDto savePlayer(Long playerId) {
        JsonNode response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/players")
                        .path("/profiles")
                        .queryParam("player", playerId)
                        .build()
                )
                .retrieve()
                .body(JsonNode.class);

        JsonNode playerData = response.get("response").get(0).get("player");

        Player player = Player.of(playerData.get("name").asString(), playerData.get("id").asLong());
        playerRepository.save(player);

        return PlayerSaveResponseDto.of(player);
    }






















    public TeamInfoResponseDto saveTeam(Long teamId) {
        JsonNode response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/teams")
                        .queryParam("id", teamId)
                        .build()
                )
                .retrieve()
                .body(JsonNode.class);

        JsonNode a = response.get("response").get(0);

        JsonNode team = a.get("team");

        long teamApiId = team.get("id").asLong();
        String teamName = team.get("name").asString();
        String teamCode = team.get("code").asString();
        String teamCountry = team.get("country").asString();
        String stadiumName = a.get("venue").get("name").asString();

        teamRepository.save(Team.of(teamName, teamApiId));

        return new TeamInfoResponseDto(teamName, teamCode, teamCountry, stadiumName);
    }









    public PlayerTransferInfoResponseDto savePlayerTransfer(Long playerId) {
        JsonNode response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/transfers")
                        .queryParam("player", playerId)
                        .build()
                )
                .retrieve()
                .body(JsonNode.class);

        JsonNode a = response.get("response").get(0);

        String playerName = a.get("player").get("name").asString();
        long playerApiId = a.get("player").get("id").asLong();

        JsonNode transfers = a.get("transfers");

        List<PlayerTransferResponseDto> playerTransfers = new ArrayList<>();


        Player player = Player.of(playerName, playerApiId);

        playerRepository.save(player);

        for (JsonNode transfer : transfers) {
            String date = transfer.get("date").asString();
            String type = transfer.get("type").asString();
            String inTeamName = transfer.get("teams").get("in").get("name").asString();
            long inTeamApiId = transfer.get("teams").get("in").get("id").asLong();

            String outTeamName = transfer.get("teams").get("out").get("name").asString();
            long outTeamApiId = transfer.get("teams").get("out").get("id").asLong();


            Team inTeam = Team.of(inTeamName, inTeamApiId);
            Team outTeam = Team.of(outTeamName, outTeamApiId);

            LocalDate parsedDate = LocalDate.parse(date);

            Transfer toSaveTransfer = Transfer.of(player, inTeam, outTeam, type, parsedDate);

            transferRepository.save(toSaveTransfer);

            playerTransfers.add(new PlayerTransferResponseDto(date, type, inTeamName, outTeamName));
        }

        return new PlayerTransferInfoResponseDto(playerName, playerTransfers);
    }


    public PlayersResponseDto saveTeamPlayers(Long teamId) {
        JsonNode response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/players")
                        .path("/squads")
                        .queryParam("team", teamId)
                        .build()
                )
                .retrieve()
                .body(JsonNode.class);

        JsonNode responses = response.get("response").get(0);

        int teamIdInt = responses.get("team").get("id").asInt();
        String teamName = responses.get("team").get("name").asString();

        JsonNode playersList = responses.get("players");


        List<PlayerItemResponseDto> players = new ArrayList<>();
        for (JsonNode playerInfo : playersList) {
            long playerApiId = playerInfo.get("id").asLong();
            String playerName = playerInfo.get("name").asString();
            int playerAge = playerInfo.get("age").asInt();
            int playerNumber = playerInfo.get("number").asInt();
            String playerPosition = playerInfo.get("position").asString();

            Player player = Player.of(playerName, playerApiId);

            playerRepository.save(player);

            players.add(new PlayerItemResponseDto(playerApiId, playerName, playerAge, playerNumber, playerPosition));
        }

        return new PlayersResponseDto(teamIdInt, teamName, players);
    }

    public ResponseEntity<?> saveCountryTeams(String country) {
        JsonNode response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/teams")
                        .queryParam("country", country)
                        .build()
                )
                .retrieve()
                .body(JsonNode.class);

        JsonNode responses = response.get("response");

        for (JsonNode respons : responses) {


            JsonNode team = respons.get("team");

            Team findTeam = Team.of(team.get("name").asString(), team.get("id").asLong());

            teamRepository.save(findTeam);
        }


        return ResponseEntity.ok().body(responses);

    }


}
