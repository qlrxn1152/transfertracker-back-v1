package com.dhoon.transfertracker.external.football.client;

import com.dhoon.transfertracker.TeamTransferData;
import com.dhoon.transfertracker.external.football.dto.PlayerSaveResponseDto;
import com.dhoon.transfertracker.external.football.dto.TeamSaveResponseDto;
import com.dhoon.transfertracker.internal.domain.LeagueCode;
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
        Player player = syncPlayerFromExternalApi(playerApiId);

        return PlayerSaveResponseDto.of(player);
    }

    /**
     * 외부 API 를 호출해서, 해당 팀 데이터를 DB 에 저장하는 작업.
     * @param teamApiID -> 외부 API ID
     */
    public TeamSaveResponseDto syncTeam(Long teamApiID) {
        Team team = syncTeamFromExternalApi(teamApiID);

        return TeamSaveResponseDto.of(team);
    }

    /**
     * 외부 API 를 호출해서, 해당 선수의 이적 데이터를 DB 에 저장하는 작업.
     * @param playerApiId -> 외부 API ID
     */
    public String syncPlayerTransfers(Long playerApiId) {
        JsonNode node = callExternalPlayerTransferApi(playerApiId);

        String playerName = node.get("response").get(0).get("player").get("name").asString();

        savePlayerTransfers(playerApiId, playerName, node);

        return "OK";
    }

    /**
     * 외부 API 를 호출해서, 해당 팀의 선수들을 DB 에 저장하는 작업.
     * @param teamApiId -> 외부 API ID
     */
    public String syncTeamPlayers(Long teamApiId) {
        JsonNode node = callExternalTeamPlayersApi(teamApiId);
        String teamName = node.get("response").get(0).get("team").get("name").asString();

        // 팀이 존재하는지부터 확인. -> 없으면 새로저장
        getOrCreateTeam(Team.of(teamName, teamApiId));
        saveMissingTeamPlayers(node);

        return "OK";
    }


    /**
     * 외부 API 를 호출해서, 해당 팀의 선수들을 DB 에 저장하는 작업. ( 2020 년 이상의 이적정보만 저장합니다.)
     * @param teamApiId -> 외부 API ID
     */
    public String saveTeamTransfers(Long teamApiId) {
        JsonNode node = callExternalTeamTransfersApi(teamApiId);
        getPlayerAndSaveAbsentTransfers(node);

        return "OK";
    }

    public String syncLeagueTeams(LeagueCode leagueCode) {
        JsonNode node = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/teams")
                        .queryParam("league", leagueCode.getApiFootballLeagueId())
                        .queryParam("season", 2024) // 무료버전은 2022 ~ 2024 까지 요청이 가능하므로, 최신 데이터는 직접 ..
                        .build()
                )
                .retrieve()
                .body(JsonNode.class);

        JsonNode teams = node.get("response");

        for (JsonNode team : teams) {
            long teamApiId = team.get("team").get("id").asLong();
            String teamName = team.get("team").get("name").asString();

            Team t = Team.of(teamName, teamApiId, leagueCode);

            // team -> DB 에 존재하지않으면, 팀 코드와 같이 해당 팀 저장
            // team -> DB 에 존재하면, 팀 코드를 추가한 업데이트 ..

            getOrCreateTeam(t).assignTeamLeague(leagueCode);// 찾아오면 영속성 컨텍스트에 들어옴 -> 변경감지로 자동 업데이트

        }


        return "OK";
    }






















    // ---------------------------- SavePlayer -----------------------
    private Player syncPlayerFromExternalApi(Long playerApiId) {
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
    private @NonNull Team syncTeamFromExternalApi(Long teamApiID) {
        JsonNode node = callExternalTeamApi(teamApiID);

        JsonNode teamData = node.get("response").get(0).get("team");

        return getOrCreateTeam(
                Team.of(
                        teamData.get("name").asString(),
                        teamData.get("id").asLong()
                ));
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

    private @NonNull Team getOrCreateTeam(Team team) {
        return teamRepository.findByApiFootballId(team.getApiFootballId())
                .orElseGet(() -> teamRepository.save(team));
    }

    // ---------------------------- SaveTeam -----------------------




    // ---------------------------- SavePlayerTransfer -----------------------

    private void savePlayerTransfers(Long playerApiId, String playerName, JsonNode node) {
        Player player = playerRepository.findByApiFootballId(playerApiId)
                .orElseGet(() -> playerRepository.save(Player.of(playerName, playerApiId)));


        getPlayerTransfers(node)
                .forEach(
                        transfer -> {
                            TeamTransferData data = TeamTransferData.of(transfer);

                            Team inTeam = getOrCreateTeam(data.getInTeam());
                            Team outTeam = getOrCreateTeam(data.getOutTeam());

                            savePlayerTransfers(player, data, inTeam, outTeam);
                        }
                );
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



    // ---------------------------- SaveTeamPlayers -----------------------

    private void saveMissingTeamPlayers(JsonNode node) {
        // 팀에 속한 플레이어들
        JsonNode players = node.get("response").get(0).get("players");

        // 팀에 속한 플레이어들중 DB 에 없는 선수들만 저장
        players.forEach(
                player -> {
                    long playerApiId = player.get("id").asLong();
                    String playerName = player.get("name").asString();

                    playerRepository.findByApiFootballId(playerApiId)
                            .orElseGet(() -> playerRepository.save(
                                    Player.of(
                                            playerName,
                                            playerApiId
                                    ))
                            );
                }
        );
    }

    private @Nullable JsonNode callExternalTeamPlayersApi(Long teamApiId) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/players")
                        .path("/squads")
                        .queryParam("team", teamApiId)
                        .build()
                )
                .retrieve()
                .body(JsonNode.class);
    }
    // ---------------------------- SaveTeamPlayers -----------------------


    // ---------------------------- SaveTeamTransfers -----------------------

    private void getPlayerAndSaveAbsentTransfers(JsonNode node) {
        node.get("response")
                .forEach(
                        response -> {
                            long playerApiId = response.get("player").get("id").asLong();

                            Player player = playerRepository.findByApiFootballId(playerApiId)
                                    .orElseGet(() -> playerRepository.save(Player.of(response.get("player").get("name").asString(), playerApiId)));

                            saveTransferIfAbsent(response, player);
                        }
                );
    }

    private @Nullable JsonNode callExternalTeamTransfersApi(Long teamIdApiId) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/transfers")
                        .queryParam("team", teamIdApiId)
                        .build()
                )
                .retrieve()
                .body(JsonNode.class);
    }

    private void saveTransferIfAbsent(JsonNode response, Player player) {
        JsonNode transfers = response.get("transfers");

        transfers.forEach(transfer -> {
            LocalDate transferDate = LocalDate.parse(transfer.get("date").asString());

            if (transferDate.getYear() >= 2020) {
                String transferType = transfer.get("type").asString();

                long inTeamApiId = transfer.get("teams").get("in").get("id").asLong();
                String inTeamName = transfer.get("teams").get("in").get("name").asString();

                long outTeamApiId = transfer.get("teams").get("out").get("id").asLong();
                String outTeamName = transfer.get("teams").get("out").get("name").asString();

                Team inTeam = getOrCreateTeam(Team.of(inTeamName, inTeamApiId));
                Team outTeam = getOrCreateTeam(Team.of(outTeamName, outTeamApiId));

                transferRepository.findByInTeamIdAndOutTeamIdAndPlayerId(inTeam.getId(), outTeam.getId(), player.getId())
                        .orElseGet(() -> transferRepository.save(Transfer.of(player, inTeam, outTeam, transferType, transferDate)));
            }
        });





    }
    // ---------------------------- SaveTeamTransfers -----------------------


}
