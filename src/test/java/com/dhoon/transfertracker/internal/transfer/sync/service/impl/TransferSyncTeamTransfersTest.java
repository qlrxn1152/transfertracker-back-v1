package com.dhoon.transfertracker.internal.transfer.sync.service.impl;

import com.dhoon.transfertracker.etc.notification.service.MemberNotificationService;
import com.dhoon.transfertracker.external.football.client.ApiFootballHttpClient;
import com.dhoon.transfertracker.external.football.dto.TeamTransfersSaveResponseDto;
import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.exception.NotFoundTeamException;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.transfer.domain.Transfer;
import com.dhoon.transfertracker.internal.transfer.repository.TransferRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;


@DataJpaTest
@Import({
        TransferSyncOrchestrationService.class,
        TransferSyncTxService.class
})
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TransferSyncTeamTransfersTest {


    @Autowired
    TransferSyncOrchestrationService transferSyncOrchestrationService;

    @Autowired
    PlayerRepository playerRepository;

    @Autowired
    TeamRepository teamRepository;

    @Autowired
    TransferRepository transferRepository;


    @MockitoBean
    ApiFootballHttpClient apiFootballHttpClient;


    @MockitoBean
    MemberNotificationService memberNotificationService;


    private final ObjectMapper objectMapper =
            new ObjectMapper();


    @AfterEach
    void clearDatabase() {
        transferRepository.deleteAll();
        playerRepository.deleteAll();
        teamRepository.deleteAll();
    }


    // ==================================================
    // 정상
    // ==================================================

    @Test
    @DisplayName("팀 이적 정보를 동기화하면 여러 선수의 이적 정보를 저장하고 DTO를 반환한다.")
    void saveTeamTransfers_success() {

        // given
        Long teamApiId = 42L;


        teamRepository.save(
                Team.of(
                        "Arsenal",
                        teamApiId
                )
        );


        given(
                apiFootballHttpClient
                        .callExternalTeamTransfersApi(
                                teamApiId
                        )
        )
                .willReturn(
                        teamTransfersResponse()
                );


        // when
        TeamTransfersSaveResponseDto response =
                transferSyncOrchestrationService
                        .syncTeamTransfers(
                                teamApiId
                        );


        // then
        assertThat(response.getTeamName())
                .isEqualTo(
                        "Arsenal"
                );

        assertThat(response.getPlayersTransfers())
                .hasSize(2);


        assertThat(response.getPlayersTransfers())
                .extracting(
                        playerTransfers ->
                                playerTransfers.getPlayerName()
                )
                .containsExactly(
                        "Bukayo Saka",
                        "Martin Odegaard"
                );


        assertThat(playerRepository.count())
                .isEqualTo(2);

        assertThat(teamRepository.count())
                .isEqualTo(2);

        assertThat(transferRepository.count())
                .isEqualTo(2);


        then(memberNotificationService)
                .should(times(2))
                .createFavoriteTeamNotificationsForAllMembers();
    }


    // ==================================================
    // Transaction Boundary
    // ==================================================

    @Test
    @DisplayName("팀 이적 외부 API를 호출하는 동안에는 DB 트랜잭션이 활성화되어 있지 않다.")
    void saveTeamTransfers_externalApiCall_withoutTransaction() {

        // given
        Long teamApiId = 42L;


        teamRepository.save(
                Team.of(
                        "Arsenal",
                        teamApiId
                )
        );


        JsonNode apiResponse =
                teamTransfersResponse();


        given(
                apiFootballHttpClient
                        .callExternalTeamTransfersApi(
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
        transferSyncOrchestrationService
                .syncTeamTransfers(
                        teamApiId
                );


        // then
        then(apiFootballHttpClient)
                .should(times(1))
                .callExternalTeamTransfersApi(
                        teamApiId
                );


        assertThat(playerRepository.count())
                .isEqualTo(2);

        assertThat(transferRepository.count())
                .isEqualTo(2);
    }


    // ==================================================
    // 멱등성
    // ==================================================

    @Test
    @DisplayName("동일한 팀 이적 정보를 여러 번 동기화해도 중복 저장하지 않는다.")
    void saveTeamTransfers_sameData_idempotent() {

        // given
        Long teamApiId = 42L;


        teamRepository.save(
                Team.of(
                        "Arsenal",
                        teamApiId
                )
        );


        JsonNode apiResponse =
                teamTransfersResponse();


        given(
                apiFootballHttpClient
                        .callExternalTeamTransfersApi(
                                teamApiId
                        )
        )
                .willReturn(
                        apiResponse
                );


        // when
        TeamTransfersSaveResponseDto firstResponse =
                transferSyncOrchestrationService
                        .syncTeamTransfers(
                                teamApiId
                        );


        TeamTransfersSaveResponseDto secondResponse =
                transferSyncOrchestrationService
                        .syncTeamTransfers(
                                teamApiId
                        );


        // then
        assertThat(playerRepository.count())
                .isEqualTo(2);

        assertThat(teamRepository.count())
                .isEqualTo(2);

        assertThat(transferRepository.count())
                .isEqualTo(2);


        assertThat(firstResponse.getPlayersTransfers())
                .hasSize(2);

        assertThat(secondResponse.getPlayersTransfers())
                .hasSize(2);


        then(apiFootballHttpClient)
                .should(times(2))
                .callExternalTeamTransfersApi(
                        teamApiId
                );


        /*
         * 첫 번째 Sync에서
         * Saka Transfer 1건,
         * Odegaard Transfer 1건이 생성된다.
         *
         * 두 번째 Sync에서는 모두 기존 Transfer를 재사용한다.
         */
        then(memberNotificationService)
                .should(times(2))
                .createFavoriteTeamNotificationsForAllMembers();
    }


    // ==================================================
    // 빈 응답
    // ==================================================

    @Test
    @DisplayName("팀 이적 응답이 비어있으면 새로운 Player와 Transfer를 생성하지 않는다.")
    void saveTeamTransfers_emptyResponse_noNewDatabaseData() {

        // given
        Long teamApiId = 42L;


        Team arsenal =
                teamRepository.save(
                        Team.of(
                                "Arsenal",
                                teamApiId
                        )
                );


        given(
                apiFootballHttpClient
                        .callExternalTeamTransfersApi(
                                teamApiId
                        )
        )
                .willReturn(
                        teamTransfersEmptyResponse()
                );


        // when
        TeamTransfersSaveResponseDto response =
                transferSyncOrchestrationService
                        .syncTeamTransfers(
                                teamApiId
                        );


        // then
        assertThat(response.getTeamName())
                .isEqualTo(
                        "Arsenal"
                );

        assertThat(response.getPlayersTransfers())
                .isEmpty();


        assertThat(playerRepository.count())
                .isZero();

        assertThat(teamRepository.count())
                .isEqualTo(1);

        assertThat(transferRepository.count())
                .isZero();


        Team foundArsenal =
                teamRepository
                        .findByApiFootballId(
                                teamApiId
                        )
                        .orElseThrow();


        assertThat(foundArsenal.getId())
                .isEqualTo(
                        arsenal.getId()
                );


        then(memberNotificationService)
                .should(never())
                .createFavoriteTeamNotificationsForAllMembers();
    }


    // ==================================================
    // 기존 Entity 재사용
    // ==================================================

    @Test
    @DisplayName("기존 Player와 Team은 재사용하고 새로운 데이터만 저장한다.")
    void saveTeamTransfers_existingPlayerAndTeam_reuse() {

        // given
        Player existingSaka =
                playerRepository.save(
                        Player.of(
                                "Bukayo Saka",
                                10L
                        )
                );


        Team existingArsenal =
                teamRepository.save(
                        Team.of(
                                "Arsenal",
                                42L
                        )
                );


        Team existingChelsea =
                teamRepository.save(
                        Team.of(
                                "Chelsea",
                                49L
                        )
                );


        given(
                apiFootballHttpClient
                        .callExternalTeamTransfersApi(
                                42L
                        )
        )
                .willReturn(
                        teamTransfersResponse()
                );


        // when
        TeamTransfersSaveResponseDto response =
                transferSyncOrchestrationService
                        .syncTeamTransfers(
                                42L
                        );


        // then
        assertThat(response.getTeamName())
                .isEqualTo(
                        "Arsenal"
                );

        assertThat(response.getPlayersTransfers())
                .hasSize(2);


        assertThat(playerRepository.count())
                .isEqualTo(2);

        assertThat(teamRepository.count())
                .isEqualTo(2);

        assertThat(transferRepository.count())
                .isEqualTo(2);


        Player foundSaka =
                playerRepository
                        .findByApiFootballId(
                                10L
                        )
                        .orElseThrow();


        Team foundArsenal =
                teamRepository
                        .findByApiFootballId(
                                42L
                        )
                        .orElseThrow();


        Team foundChelsea =
                teamRepository
                        .findByApiFootballId(
                                49L
                        )
                        .orElseThrow();


        assertThat(foundSaka.getId())
                .isEqualTo(
                        existingSaka.getId()
                );

        assertThat(foundArsenal.getId())
                .isEqualTo(
                        existingArsenal.getId()
                );

        assertThat(foundChelsea.getId())
                .isEqualTo(
                        existingChelsea.getId()
                );
    }


    // ==================================================
    // Team 존재 여부
    // ==================================================

    @Test
    @DisplayName("DB에 Team이 존재하지 않으면 외부 API를 호출하지 않고 예외가 발생한다.")
    void saveTeamTransfers_notFoundTeam_noExternalApiCall() {

        // given
        Long teamApiId = 42L;


        // when & then
        assertThatThrownBy(() ->
                transferSyncOrchestrationService
                        .syncTeamTransfers(
                                teamApiId
                        )
        )
                .isInstanceOf(
                        NotFoundTeamException.class
                );


        /*
         * getDisplayTeamName()에서 먼저 실패한다.
         *
         * 따라서 존재하지 않는 Team에 대해
         * API-Football까지 요청하면 안 된다.
         */
        then(apiFootballHttpClient)
                .should(never())
                .callExternalTeamTransfersApi(
                        teamApiId
                );


        assertThat(playerRepository.count())
                .isZero();

        assertThat(teamRepository.count())
                .isZero();

        assertThat(transferRepository.count())
                .isZero();


        then(memberNotificationService)
                .should(never())
                .createFavoriteTeamNotificationsForAllMembers();
    }


    // ==================================================
    // 외부 API 실패
    // ==================================================

    @Test
    @DisplayName("팀 이적 외부 API 호출에 실패하면 새로운 DB 데이터는 생성되지 않는다.")
    void saveTeamTransfers_externalApiFail_noNewDatabaseData() {

        // given
        Long teamApiId = 42L;


        Team arsenal =
                teamRepository.save(
                        Team.of(
                                "Arsenal",
                                teamApiId
                        )
                );


        given(
                apiFootballHttpClient
                        .callExternalTeamTransfersApi(
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
                transferSyncOrchestrationService
                        .syncTeamTransfers(
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
                .callExternalTeamTransfersApi(
                        teamApiId
                );


        assertThat(playerRepository.count())
                .isZero();

        /*
         * Sync 실행 전 존재하던 Arsenal은 유지된다.
         */
        assertThat(teamRepository.count())
                .isEqualTo(1);

        assertThat(transferRepository.count())
                .isZero();


        Team foundArsenal =
                teamRepository
                        .findByApiFootballId(
                                teamApiId
                        )
                        .orElseThrow();


        assertThat(foundArsenal.getId())
                .isEqualTo(
                        arsenal.getId()
                );


        then(memberNotificationService)
                .should(never())
                .createFavoriteTeamNotificationsForAllMembers();
    }


    // ==================================================
    // Player 단위 Transaction
    // ==================================================

    @Test
    @DisplayName("중간 선수의 이적 저장이 실패하면 이전 선수는 유지되고 이후 선수는 처리되지 않는다.")
    void saveTeamTransfers_secondPlayerFail_partialCommitAndStop() {

        // given
        Long teamApiId = 42L;


        teamRepository.save(
                Team.of(
                        "Arsenal",
                        teamApiId
                )
        );


        given(
                apiFootballHttpClient
                        .callExternalTeamTransfersApi(
                                teamApiId
                        )
        )
                .willReturn(
                        teamTransfersSecondPlayerInvalidResponse()
                );


        // when & then
        assertThatThrownBy(() ->
                transferSyncOrchestrationService
                        .syncTeamTransfers(
                                teamApiId
                        )
        )
                .isInstanceOf(
                        DateTimeParseException.class
                );


        /*
         * ==========================================
         * Player #1 - Bukayo Saka
         * ==========================================
         *
         * 자신의 Transaction을 정상적으로 완료했다.
         *
         * 따라서 이후 Player #2가 실패해도
         * 이미 Commit된 Saka 데이터는 유지되어야 한다.
         */
        assertThat(
                playerRepository
                        .findByApiFootballId(
                                10L
                        )
        )
                .isPresent();


        /*
         * ==========================================
         * Player #2 - Martin Odegaard
         * ==========================================
         *
         * getOrCreatePlayer()에서 Player를 저장한 뒤
         * invalid-date Parsing에서 실패한다.
         *
         * 하지만 Player 생성과 Transfer 처리가
         * 같은 Transaction 안이므로 전부 Rollback된다.
         */
        assertThat(
                playerRepository
                        .findByApiFootballId(
                                20L
                        )
        )
                .isEmpty();


        /*
         * ==========================================
         * Player #3 - Declan Rice
         * ==========================================
         *
         * Player #2의 예외가 OrchestrationService까지
         * 전파되면서 forEach 자체가 중단된다.
         *
         * 따라서 Player #3은 처리되지 않는다.
         */
        assertThat(
                playerRepository
                        .findByApiFootballId(
                                30L
                        )
        )
                .isEmpty();


        assertThat(playerRepository.count())
                .isEqualTo(1);


        /*
         * Arsenal
         * → Sync 전부터 존재
         *
         * Chelsea
         * → Saka Transaction에서 생성 후 Commit
         */
        assertThat(teamRepository.count())
                .isEqualTo(2);


        /*
         * 정상 Commit된 Saka Transfer만 존재한다.
         */
        assertThat(transferRepository.count())
                .isEqualTo(1);


        Player saka =
                playerRepository
                        .findByApiFootballId(
                                10L
                        )
                        .orElseThrow();


        List<Transfer> sakaTransfers =
                transferRepository
                        .findAllByPlayerId(
                                saka.getId()
                        );


        assertThat(sakaTransfers)
                .hasSize(1);


        Transfer transfer =
                sakaTransfers.get(0);


        assertThat(transfer.getTransferDate())
                .isEqualTo(
                        LocalDate.of(
                                2026,
                                7,
                                1
                        )
                );

        assertThat(transfer.getInTeam().getTeamName())
                .isEqualTo(
                        "Arsenal"
                );

        assertThat(transfer.getOutTeam().getTeamName())
                .isEqualTo(
                        "Chelsea"
                );


        /*
         * 성공한 Player #1에서만
         * 새로운 Transfer가 생성됐으므로 1회 호출된다.
         */
        then(memberNotificationService)
                .should(times(1))
                .createFavoriteTeamNotificationsForAllMembers();
    }


    // ==================================================
    // Fixture
    // ==================================================

    private JsonNode teamTransfersResponse() {

        return readJson(
                """
                {
                  "response": [
                    {
                      "player": {
                        "id": 10,
                        "name": "Bukayo Saka"
                      },
                      "transfers": [
                        {
                          "date": "2026-07-01",
                          "type": "Transfer",
                          "teams": {
                            "in": {
                              "id": 42,
                              "name": "Arsenal"
                            },
                            "out": {
                              "id": 49,
                              "name": "Chelsea"
                            }
                          }
                        }
                      ]
                    },
                    {
                      "player": {
                        "id": 20,
                        "name": "Martin Odegaard"
                      },
                      "transfers": [
                        {
                          "date": "2025-07-01",
                          "type": "Transfer",
                          "teams": {
                            "in": {
                              "id": 42,
                              "name": "Arsenal"
                            },
                            "out": {
                              "id": 49,
                              "name": "Chelsea"
                            }
                          }
                        }
                      ]
                    }
                  ]
                }
                """
        );
    }


    private JsonNode teamTransfersEmptyResponse() {

        return readJson(
                """
                {
                  "response": []
                }
                """
        );
    }


    private JsonNode teamTransfersSecondPlayerInvalidResponse() {

        return readJson(
                """
                {
                  "response": [
                    {
                      "player": {
                        "id": 10,
                        "name": "Bukayo Saka"
                      },
                      "transfers": [
                        {
                          "date": "2026-07-01",
                          "type": "Transfer",
                          "teams": {
                            "in": {
                              "id": 42,
                              "name": "Arsenal"
                            },
                            "out": {
                              "id": 49,
                              "name": "Chelsea"
                            }
                          }
                        }
                      ]
                    },
                    {
                      "player": {
                        "id": 20,
                        "name": "Martin Odegaard"
                      },
                      "transfers": [
                        {
                          "date": "invalid-date",
                          "type": "Transfer",
                          "teams": {
                            "in": {
                              "id": 42,
                              "name": "Arsenal"
                            },
                            "out": {
                              "id": 49,
                              "name": "Chelsea"
                            }
                          }
                        }
                      ]
                    },
                    {
                      "player": {
                        "id": 30,
                        "name": "Declan Rice"
                      },
                      "transfers": [
                        {
                          "date": "2024-07-01",
                          "type": "Transfer",
                          "teams": {
                            "in": {
                              "id": 42,
                              "name": "Arsenal"
                            },
                            "out": {
                              "id": 49,
                              "name": "Chelsea"
                            }
                          }
                        }
                      ]
                    }
                  ]
                }
                """
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