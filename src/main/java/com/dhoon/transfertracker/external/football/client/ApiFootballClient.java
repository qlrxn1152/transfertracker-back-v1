package com.dhoon.transfertracker.external.football.client;

import com.dhoon.transfertracker.exception.AlreadyExistTransferException;
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
     * @param playerApiId -> 외부 API ID
     */
    public PlayerSaveResponseDto savePlayer(Long playerApiId) {
        JsonNode node = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/players")
                        .path("/profiles")
                        .queryParam("player", playerApiId)
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
     * @param teamApiID -> 외부 API ID
     */
    public TeamSaveResponseDto saveTeam(Long teamApiID) {
        JsonNode node = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/teams")
                        .queryParam("id", teamApiID)
                        .build()
                )
                .retrieve()
                .body(JsonNode.class);

        JsonNode teamData = node.get("response").get(0).get("team");

        Team team = Team.of(teamData.get("name").asString(), teamData.get("id").asLong());
        teamRepository.save(team);

        return TeamSaveResponseDto.of(team);
    }



    /**
     * 외부 API 를 호출해서, 해당 선수의 이적 데이터를 DB 에 저장하는 작업.
     * @param playerApiId -> 외부 API ID
     */
    public TransferSaveResponseDto savePlayerTransfer(Long playerApiId) {
        JsonNode node = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/players")
                        .path("/teams")
                        .queryParam("player", playerApiId)
                        .build()
                )
                .retrieve()
                .body(JsonNode.class);

        JsonNode teams = node.get("response");

        // 해당 플레이어가 속했던 팀들을 Team 테이블에서 조회 -> 존재하면 통과 / 존재하지 않으면, 해당 팀을 Team 테이블에 저장.
        for (JsonNode team : teams) {
            long teamAPIId = team.get("team").get("id").asLong();

            if ( !teamRepository.existsByApiFootballId(teamAPIId) ) {
                TeamSaveResponseDto teamDto = saveTeam(teamAPIId);
                log.info("이적 정보를 위한 팀이 등록이 되었습니다. teamName = {}", teamDto.getTeamName());
            }
        }
        // 해당 플레이어가 속했던 팀들이 모두 Team 테이블에 존재.

        // 위에 까지는, 플레이어가 속했던 팀들이 DB 에 존재하는지를 확인하는 ...


        // 아래에서부터는, 해당 선수의 이적정보들을 조회 -> transfer 전용 외부 API 호출.

        JsonNode node2 = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/transfers")
                        .queryParam("player", playerApiId)
                        .build()
                )
                .retrieve()
                .body(JsonNode.class);

        JsonNode transfers = node2.get("response").get(0).get("transfers");

        playerRepository.findByApiFootballId(playerApiId)
                .ifPresentOrElse(
                        player -> {},
                        () -> savePlayer(playerApiId)
                );

        Player player = playerRepository.findByApiFootballId(playerApiId).orElseThrow();


        for (JsonNode transfer : transfers) {
            // transfer -> 1개의 이적정보. ( OutTeam -> InTeam .. )
            String transferDate = transfer.get("date").asString();
            String transferType = transfer.get("type").asString();

            Long inTeamAPIID = transfer.get("teams").get("in").get("id").asLong();
            Long outTeamAPIID = transfer.get("teams").get("out").get("id").asLong();

            Team inTeam = teamRepository.findByApiFootballId(inTeamAPIID).orElseThrow();
            Team outTeam = teamRepository.findByApiFootballId(outTeamAPIID).orElseThrow();

            Transfer t = Transfer.of(player, inTeam, outTeam, transferType, LocalDate.parse(transferDate));

            // transfer 이미 존재하면, 저장못하게.
            if (transferRepository.existsByInTeamIdAndOutTeamIdAndPlayerId(inTeam.getId(), outTeam.getId(), player.getId())) {
                throw new AlreadyExistTransferException();
            }


            transferRepository.save(t); // 우선 단건저장 -> 이후에, 이적정보들을 모아서 한번에 저장 .. ( 성능개선 )
        }

        log.info("[{}] 의 이적정보가 등록되었습니다.", player.getPlayerName());

        return TransferSaveResponseDto.of(player);
    }


    /**
     * 외부 API 를 호출해서, 해당 팀의 선수들을 DB 에 저장하는 작업.
     * @param teamApiID -> 외부 API ID
     */
    public TeamPlayersSaveResponseDto saveTeamPlayers(Long teamApiID) {
        JsonNode node = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/players")
                        .path("/squads")
                        .queryParam("team", teamApiID)
                        .build()
                )
                .retrieve()
                .body(JsonNode.class);

        // 팀이 존재하는지부터 확인.

        if ( !teamRepository.existsByApiFootballId(teamApiID) ) {
            saveTeam(teamApiID);
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









}
