package com.dhoon.transfertracker.internal.teamplayer.sync.service.impl;

import com.dhoon.transfertracker.external.football.client.ApiFootballHttpClient;
import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.teamplayer.domain.TeamPlayer;
import com.dhoon.transfertracker.internal.teamplayer.repository.TeamPlayerRepository;
import com.dhoon.transfertracker.internal.teamplayer.sync.dto.response.TeamPlayerSaveResponseDto;
import com.dhoon.transfertracker.internal.teamplayer.sync.dto.response.TeamPlayersSaveResponseDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;


@DataJpaTest
@Import({
        TeamPlayerSyncOrchestrationService.class,
        TeamPlayerSyncTxService.class
})
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TeamPlayerSyncTest {


    @Autowired
    TeamPlayerSyncOrchestrationService teamPlayerSyncOrchestrationService;

    @Autowired
    TeamPlayerRepository teamPlayerRepository;

    @Autowired
    PlayerRepository playerRepository;

    @Autowired
    TeamRepository teamRepository;


    @MockitoBean
    ApiFootballHttpClient apiFootballHttpClient;


    private final ObjectMapper objectMapper =
            new ObjectMapper();


    @AfterEach
    void clearDatabase() {
        teamPlayerRepository.deleteAll();
        playerRepository.deleteAll();
        teamRepository.deleteAll();
    }


    // ==================================================
    // 정상
    // ==================================================

    @Test
    @DisplayName("팀 선수 정보를 동기화하면 Team, Player, TeamPlayer가 저장된다.")
    void syncTeamPlayers_success() {

        // given
        Long teamApiId = 42L;


        given(
                apiFootballHttpClient
                        .callExternalTeamPlayersApi(
                                teamApiId
                        )
        )
                .willReturn(
                        teamPlayersResponse()
                );


        // when
        TeamPlayersSaveResponseDto response =
                teamPlayerSyncOrchestrationService
                        .syncTeamPlayers(
                                teamApiId
                        );


        // then
        assertThat(teamRepository.count())
                .isEqualTo(1);

        assertThat(playerRepository.count())
                .isEqualTo(2);

        assertThat(teamPlayerRepository.count())
                .isEqualTo(2);


        Team arsenal =
                teamRepository
                        .findByApiFootballId(
                                42L
                        )
                        .orElseThrow();


        assertThat(arsenal.getTeamName())
                .isEqualTo(
                        "Arsenal"
                );


        assertThat(
                playerRepository
                        .findByApiFootballId(
                                10L
                        )
        )
                .isPresent();

        assertThat(
                playerRepository
                        .findByApiFootballId(
                                20L
                        )
        )
                .isPresent();


        assertThat(response.getPlayers())
                .hasSize(2);


        assertThat(response.getPlayers())
                .extracting(
                        TeamPlayerSaveResponseDto::getPlayerName
                )
                .containsExactly(
                        "Bukayo Saka",
                        "Martin Odegaard"
                );


        assertThat(response.getPlayers())
                .extracting(
                        TeamPlayerSaveResponseDto::getTeamName
                )
                .containsOnly(
                        "Arsenal"
                );


        assertThat(response.getPlayers())
                .extracting(
                        TeamPlayerSaveResponseDto::getTeamId
                )
                .containsOnly(
                        arsenal.getId()
                );
    }


    // ==================================================
    // Transaction Boundary
    // ==================================================

    @Test
    @DisplayName("팀 선수 외부 API를 호출하는 동안에는 DB 트랜잭션이 활성화되어 있지 않다.")
    void syncTeamPlayers_externalApiCall_withoutTransaction() {

        // given
        Long teamApiId = 42L;

        JsonNode apiResponse =
                teamPlayersResponse();


        given(
                apiFootballHttpClient
                        .callExternalTeamPlayersApi(
                                teamApiId
                        )
        )
                .willAnswer(invocation -> {

                    assertThat(
                            TransactionSynchronizationManager
                                    .isActualTransactionActive()
                    )
                            .isFalse();

                    return apiResponse;
                });


        // when
        teamPlayerSyncOrchestrationService
                .syncTeamPlayers(
                        teamApiId
                );


        // then
        then(apiFootballHttpClient)
                .should(times(1))
                .callExternalTeamPlayersApi(
                        teamApiId
                );


        assertThat(teamRepository.count())
                .isEqualTo(1);

        assertThat(playerRepository.count())
                .isEqualTo(2);

        assertThat(teamPlayerRepository.count())
                .isEqualTo(2);
    }


    // ==================================================
    // 멱등성
    // ==================================================

    @Test
    @DisplayName("동일한 팀 선수 목록을 여러 번 동기화해도 중복 저장하지 않는다.")
    void syncTeamPlayers_sameData_idempotent() {

        // given
        Long teamApiId = 42L;

        JsonNode apiResponse =
                teamPlayersResponse();


        given(
                apiFootballHttpClient
                        .callExternalTeamPlayersApi(
                                teamApiId
                        )
        )
                .willReturn(
                        apiResponse
                );


        // when
        TeamPlayersSaveResponseDto firstResponse =
                teamPlayerSyncOrchestrationService
                        .syncTeamPlayers(
                                teamApiId
                        );


        TeamPlayersSaveResponseDto secondResponse =
                teamPlayerSyncOrchestrationService
                        .syncTeamPlayers(
                                teamApiId
                        );


        // then
        assertThat(teamRepository.count())
                .isEqualTo(1);

        assertThat(playerRepository.count())
                .isEqualTo(2);

        assertThat(teamPlayerRepository.count())
                .isEqualTo(2);


        assertThat(firstResponse.getPlayers())
                .hasSize(2);

        assertThat(secondResponse.getPlayers())
                .hasSize(2);


        then(apiFootballHttpClient)
                .should(times(2))
                .callExternalTeamPlayersApi(
                        teamApiId
                );
    }


    // ==================================================
    // 기존 Entity 재사용
    // ==================================================

    @Test
    @DisplayName("이미 존재하는 Team과 Player는 재사용하고 새로운 선수만 저장한다.")
    void syncTeamPlayers_existingTeamAndPlayer_reuse() {

        // given
        Team existingArsenal =
                teamRepository.save(
                        Team.of(
                                "Arsenal",
                                42L
                        )
                );


        Player existingSaka =
                playerRepository.save(
                        Player.of(
                                "Bukayo Saka",
                                10L
                        )
                );


        given(
                apiFootballHttpClient
                        .callExternalTeamPlayersApi(
                                42L
                        )
        )
                .willReturn(
                        teamPlayersResponse()
                );


        // when
        TeamPlayersSaveResponseDto response =
                teamPlayerSyncOrchestrationService
                        .syncTeamPlayers(
                                42L
                        );


        // then
        assertThat(teamRepository.count())
                .isEqualTo(1);

        assertThat(playerRepository.count())
                .isEqualTo(2);

        assertThat(teamPlayerRepository.count())
                .isEqualTo(2);


        Team foundArsenal =
                teamRepository
                        .findByApiFootballId(
                                42L
                        )
                        .orElseThrow();


        Player foundSaka =
                playerRepository
                        .findByApiFootballId(
                                10L
                        )
                        .orElseThrow();


        assertThat(foundArsenal.getId())
                .isEqualTo(
                        existingArsenal.getId()
                );

        assertThat(foundSaka.getId())
                .isEqualTo(
                        existingSaka.getId()
                );


        assertThat(response.getPlayers())
                .hasSize(2);
    }


    // ==================================================
    // 빈 응답
    // ==================================================

    @Test
    @DisplayName("선수 목록이 비어있으면 Team, Player, TeamPlayer를 저장하지 않는다.")
    void syncTeamPlayers_emptyPlayers_noDatabaseChange() {

        // given
        Long teamApiId = 42L;


        given(
                apiFootballHttpClient
                        .callExternalTeamPlayersApi(
                                teamApiId
                        )
        )
                .willReturn(
                        teamPlayersEmptyResponse()
                );


        // when
        TeamPlayersSaveResponseDto response =
                teamPlayerSyncOrchestrationService
                        .syncTeamPlayers(
                                teamApiId
                        );


        // then
        assertThat(teamRepository.count())
                .isZero();

        assertThat(playerRepository.count())
                .isZero();

        assertThat(teamPlayerRepository.count())
                .isZero();


        assertThat(response.getPlayers())
                .isEmpty();
    }


    // ==================================================
    // 외부 API 실패
    // ==================================================

    @Test
    @DisplayName("팀 선수 외부 API 호출에 실패하면 DB 데이터는 변경되지 않는다.")
    void syncTeamPlayers_externalApiFail_noDatabaseChange() {

        // given
        Long teamApiId = 42L;


        given(
                apiFootballHttpClient
                        .callExternalTeamPlayersApi(
                                teamApiId
                        )
        )
                .willThrow(
                        new RuntimeException(
                                "API-Football 호출 실패"
                        )
                );


        // when & then
        assertThatThrownBy(() ->
                teamPlayerSyncOrchestrationService
                        .syncTeamPlayers(
                                teamApiId
                        )
        )
                .isInstanceOf(
                        RuntimeException.class
                )
                .hasMessage(
                        "API-Football 호출 실패"
                );


        then(apiFootballHttpClient)
                .should(times(1))
                .callExternalTeamPlayersApi(
                        teamApiId
                );


        assertThat(teamRepository.count())
                .isZero();

        assertThat(playerRepository.count())
                .isZero();

        assertThat(teamPlayerRepository.count())
                .isZero();
    }


    // ==================================================
    // Player 단위 Transaction
    // ==================================================

    @Test
    @DisplayName("여러 선수 중 뒤의 선수 처리에 실패하면 앞에서 완료된 선수 데이터는 유지된다.")
    void syncTeamPlayers_secondPlayerFail_partialCommit() {

        // given
        Long teamApiId = 42L;


        given(
                apiFootballHttpClient
                        .callExternalTeamPlayersApi(
                                teamApiId
                        )
        )
                .willReturn(
                        teamPlayersSecondPlayerInvalidResponse()
                );


        // when & then
        assertThatThrownBy(() ->
                teamPlayerSyncOrchestrationService
                        .syncTeamPlayers(
                                teamApiId
                        )
        )
                .isInstanceOf(
                        NullPointerException.class
                );


        /*
         * Player #1
         *
         * Saka 처리는 이미 별도 Transaction에서
         * 정상 Commit되었다.
         */
        assertThat(teamRepository.count())
                .isEqualTo(1);

        assertThat(playerRepository.count())
                .isEqualTo(1);

        assertThat(teamPlayerRepository.count())
                .isEqualTo(1);


        Player saka =
                playerRepository
                        .findByApiFootballId(
                                10L
                        )
                        .orElseThrow();


        Team arsenal =
                teamRepository
                        .findByApiFootballId(
                                42L
                        )
                        .orElseThrow();


        TeamPlayer teamPlayer =
                teamPlayerRepository
                        .findByPlayerIdAndTeamId(
                                saka.getId(),
                                arsenal.getId()
                        )
                        .orElseThrow();


        assertThat(teamPlayer.getPlayer().getId())
                .isEqualTo(
                        saka.getId()
                );

        assertThat(teamPlayer.getTeam().getId())
                .isEqualTo(
                        arsenal.getId()
                );


        /*
         * Player #2는 처리 중 실패했다.
         *
         * 해당 선수 Transaction은 Rollback된다.
         */
        assertThat(
                playerRepository
                        .findByApiFootballId(
                                20L
                        )
        )
                .isEmpty();
    }


    // ==================================================
    // 현재 소속팀 충돌
    // ==================================================

    @Test
    @DisplayName("이미 다른 팀에 소속된 선수는 새로운 TeamPlayer로 중복 연결할 수 없다.")
    void syncTeamPlayers_playerAlreadyBelongsToOtherTeam_fail() {

        // given
        Team chelsea =
                teamRepository.save(
                        Team.of(
                                "Chelsea",
                                49L
                        )
                );


        Player saka =
                playerRepository.save(
                        Player.of(
                                "Bukayo Saka",
                                10L
                        )
                );


        TeamPlayer existingTeamPlayer =
                teamPlayerRepository.save(
                        TeamPlayer.of(
                                chelsea,
                                saka
                        )
                );


        JsonNode apiResponse =
                singlePlayerResponse(
                        42L,
                        "Arsenal",
                        10L,
                        "Bukayo Saka"
                );


        given(
                apiFootballHttpClient
                        .callExternalTeamPlayersApi(
                                42L
                        )
        )
                .willReturn(
                        apiResponse
                );


        // when & then
        assertThatThrownBy(() ->
                teamPlayerSyncOrchestrationService
                        .syncTeamPlayers(
                                42L
                        )
        )
                .isInstanceOf(
                        DataIntegrityViolationException.class
                );


        /*
         * 기존 Chelsea 소속 관계는 그대로 유지된다.
         *
         * Arsenal을 생성하고 새로운 TeamPlayer를 만들려던
         * Transaction은 전체 Rollback되어야 한다.
         */
        assertThat(teamRepository.count())
                .isEqualTo(1);

        assertThat(playerRepository.count())
                .isEqualTo(1);

        assertThat(teamPlayerRepository.count())
                .isEqualTo(1);


        assertThat(
                teamRepository
                        .findByApiFootballId(
                                42L
                        )
        )
                .isEmpty();


        TeamPlayer foundTeamPlayer =
                teamPlayerRepository
                        .findById(
                                existingTeamPlayer.getId()
                        )
                        .orElseThrow();


        assertThat(foundTeamPlayer.getTeam().getId())
                .isEqualTo(
                        chelsea.getId()
                );

        assertThat(foundTeamPlayer.getPlayer().getId())
                .isEqualTo(
                        saka.getId()
                );
    }


    // ==================================================
    // Fixture
    // ==================================================

    private JsonNode teamPlayersResponse() {

        return readJson(
                """
                {
                  "response": [
                    {
                      "team": {
                        "id": 42,
                        "name": "Arsenal"
                      },
                      "players": [
                        {
                          "id": 10,
                          "name": "Bukayo Saka"
                        },
                        {
                          "id": 20,
                          "name": "Martin Odegaard"
                        }
                      ]
                    }
                  ]
                }
                """
        );
    }


    private JsonNode teamPlayersEmptyResponse() {

        return readJson(
                """
                {
                  "response": [
                    {
                      "team": {
                        "id": 42,
                        "name": "Arsenal"
                      },
                      "players": []
                    }
                  ]
                }
                """
        );
    }


    private JsonNode teamPlayersSecondPlayerInvalidResponse() {

        return readJson(
                """
                {
                  "response": [
                    {
                      "team": {
                        "id": 42,
                        "name": "Arsenal"
                      },
                      "players": [
                        {
                          "id": 10,
                          "name": "Bukayo Saka"
                        },
                        {
                          "id": 20
                        }
                      ]
                    }
                  ]
                }
                """
        );
    }


    private JsonNode singlePlayerResponse(
            Long teamApiId,
            String teamName,
            Long playerApiId,
            String playerName
    ) {

        return readJson(
                """
                {
                  "response": [
                    {
                      "team": {
                        "id": %d,
                        "name": "%s"
                      },
                      "players": [
                        {
                          "id": %d,
                          "name": "%s"
                        }
                      ]
                    }
                  ]
                }
                """.formatted(
                        teamApiId,
                        teamName,
                        playerApiId,
                        playerName
                )
        );
    }


    private JsonNode readJson(
            String json
    ) {

        try {

            return objectMapper.readTree(
                    json
            );

        } catch (Exception e) {

            throw new IllegalStateException(
                    "테스트 JSON 생성 실패",
                    e
            );
        }
    }
}