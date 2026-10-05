package com.dhoon.transfertracker.external.football.service;

import com.dhoon.transfertracker.external.football.client.ApiFootballHttpClient;
import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.teamplayer.repository.TeamPlayerRepository;
import com.dhoon.transfertracker.internal.transfer.domain.Transfer;
import com.dhoon.transfertracker.internal.transfer.repository.TransferRepository;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.times;


@DataJpaTest
@Import({
        FootballSyncService.class,
        FootballSyncTxService.class
})
@ActiveProfiles("test")

/*
 * [신규 - 중요]
 *
 * @DataJpaTest의 기본 Test Transaction을 비활성화한다.
 *
 * 실제 구조:
 *
 * FootballSyncService
 *      → API-Football 호출
 *      → Transaction X
 *
 * FootballSyncTxService
 *      → 선수 한 명의 Player / Team / Transfer 저장
 *      → Transaction O
 *
 * saveTeamTransfers()는 여러 선수를 순회하면서
 * FootballSyncTxService.getOrCreatePlayerTransfers()를
 * 선수마다 한 번씩 호출한다.
 *
 * 따라서 실제 Transaction 경계와
 * 선수 단위 Partial Commit을 검증하기 위해
 * 테스트 자체의 Transaction은 사용하지 않는다.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FootballSyncTeamTransfersServiceTest {


    @Autowired
    FootballSyncService footballSyncService;


    @Autowired
    PlayerRepository playerRepository;


    @Autowired
    TeamRepository teamRepository;


    @Autowired
    TransferRepository transferRepository;


    @Autowired
    TeamPlayerRepository teamPlayerRepository;


    /*
     * 실제 API-Football 서버는 호출하지 않는다.
     *
     * 테스트에서
     *
     * - 정상 응답
     * - 빈 응답
     * - 외부 API 실패
     * - 특정 선수 데이터 실패
     *
     * 상황을 직접 만든다.
     */
    @MockitoBean
    ApiFootballHttpClient footballRestClient;


    private final ObjectMapper objectMapper =
            new ObjectMapper();


    @BeforeEach
    void clearDatabase() {

        /*
         * 테스트 자체가 Transaction으로 감싸져 있지 않으므로
         * 테스트 사이 데이터 격리를 위해 직접 삭제한다.
         *
         * FK를 참조하는 Entity부터 삭제한다.
         */
        transferRepository.deleteAll();
        teamPlayerRepository.deleteAll();

        playerRepository.deleteAll();
        teamRepository.deleteAll();
    }


    // ==================================================
    // 정상 상황
    // ==================================================


    /*
     * [신규]
     *
     * 나타내는 상황:
     *
     * Arsenal 팀의 이적 데이터 API에서
     *
     * Bukayo Saka
     * Martin Odegaard
     *
     * 두 선수의 이적 정보를 정상적으로 반환한다.
     *
     * 기대 결과:
     *
     * Player   2건
     * Team     2건
     * Transfer 2건
     *
     * 이 저장되어야 한다.
     */
    @Test
    @DisplayName(
            "팀 이적 정보를 동기화하면 여러 선수의 Player, Team, Transfer가 저장된다."
    )
    void saveTeamTransfers_success() {

        // given
        Long teamApiId = 42L;


        JsonNode apiResponse =
                teamTransfersResponse();


        given(
                footballRestClient
                        .callExternalTeamTransfersApi(
                                teamApiId
                        )
        )
                .willReturn(
                        apiResponse
                );


        // when
        String response =
                footballSyncService
                        .saveTeamTransfers(
                                teamApiId
                        );


        // then
        assertThat(response)
                .isEqualTo(
                        "FootballSyncService.saveTeamTransfers"
                );


        assertThat(playerRepository.count())
                .isEqualTo(2);

        assertThat(teamRepository.count())
                .isEqualTo(2);

        assertThat(transferRepository.count())
                .isEqualTo(2);


        Player saka =
                playerRepository
                        .findByApiFootballId(
                                10L
                        )
                        .orElseThrow();


        Player odegaard =
                playerRepository
                        .findByApiFootballId(
                                20L
                        )
                        .orElseThrow();


        assertThat(saka.getPlayerName())
                .isEqualTo(
                        "Bukayo Saka"
                );

        assertThat(odegaard.getPlayerName())
                .isEqualTo(
                        "Martin Odegaard"
                );


        List<Transfer> sakaTransfers =
                transferRepository
                        .findAllByPlayerId(
                                saka.getId()
                        );


        List<Transfer> odegaardTransfers =
                transferRepository
                        .findAllByPlayerId(
                                odegaard.getId()
                        );


        assertThat(sakaTransfers)
                .hasSize(1);

        assertThat(odegaardTransfers)
                .hasSize(1);


        assertThat(
                sakaTransfers
                        .get(0)
                        .getTransferDate()
        )
                .isEqualTo(
                        LocalDate.of(
                                2026,
                                7,
                                1
                        )
                );


        assertThat(
                odegaardTransfers
                        .get(0)
                        .getTransferDate()
        )
                .isEqualTo(
                        LocalDate.of(
                                2025,
                                7,
                                1
                        )
                );
    }


    /*
     * [신규 - 중요]
     *
     * 나타내는 상황:
     *
     * FootballSyncService가
     * 팀의 이적 정보를 API-Football에 요청하는 순간.
     *
     * 기대 결과:
     *
     * 외부 HTTP 호출을 기다리는 동안에는
     * DB Transaction이 활성화되어 있지 않아야 한다.
     */
    @Test
    @DisplayName(
            "팀 이적 API를 호출하는 동안에는 DB 트랜잭션이 활성화되어 있지 않다."
    )
    void saveTeamTransfers_externalApiCall_withoutTransaction() {

        // given
        Long teamApiId = 42L;


        JsonNode apiResponse =
                teamTransfersResponse();


        given(
                footballRestClient
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
        footballSyncService
                .saveTeamTransfers(
                        teamApiId
                );


        // then
        then(footballRestClient)
                .should(times(1))
                .callExternalTeamTransfersApi(
                        teamApiId
                );


        assertThat(playerRepository.count())
                .isEqualTo(2);

        assertThat(transferRepository.count())
                .isEqualTo(2);
    }


    /*
     * [신규 - 중요]
     *
     * 나타내는 상황:
     *
     * 동일한 팀 이적 API 응답을 가지고
     * saveTeamTransfers()를 두 번 실행한다.
     *
     * 기대 결과:
     *
     * 두 번째 동기화에서도
     *
     * 기존 Player
     * 기존 Team
     * 기존 Transfer
     *
     * 를 재사용해야 한다.
     *
     * 따라서 데이터 건수는 증가하지 않아야 한다.
     */
    @Test
    @DisplayName(
            "동일한 팀 이적 정보를 여러 번 동기화해도 중복 저장하지 않는다."
    )
    void saveTeamTransfers_sameData_idempotent() {

        // given
        Long teamApiId = 42L;


        JsonNode apiResponse =
                teamTransfersResponse();


        given(
                footballRestClient
                        .callExternalTeamTransfersApi(
                                teamApiId
                        )
        )
                .willReturn(
                        apiResponse
                );


        // when
        footballSyncService
                .saveTeamTransfers(
                        teamApiId
                );


        footballSyncService
                .saveTeamTransfers(
                        teamApiId
                );


        // then
        assertThat(playerRepository.count())
                .isEqualTo(2);

        assertThat(teamRepository.count())
                .isEqualTo(2);

        assertThat(transferRepository.count())
                .isEqualTo(2);


        then(footballRestClient)
                .should(times(2))
                .callExternalTeamTransfersApi(
                        teamApiId
                );
    }


    /*
     * [신규]
     *
     * 나타내는 상황:
     *
     * API-Football에서 팀에 대한 이적 데이터 요청은 성공했지만
     * response 배열이 비어있는 상황.
     *
     * 기대 결과:
     *
     * 반복문에서 처리할 선수가 없으므로
     *
     * Player
     * Team
     * Transfer
     *
     * 어느 것도 생성되지 않아야 한다.
     */
    @Test
    @DisplayName(
            "팀 이적 응답이 비어있으면 DB 데이터를 생성하지 않는다."
    )
    void saveTeamTransfers_emptyResponse_noDatabaseChange() {

        // given
        Long teamApiId = 42L;


        JsonNode apiResponse =
                teamTransfersEmptyResponse();


        given(
                footballRestClient
                        .callExternalTeamTransfersApi(
                                teamApiId
                        )
        )
                .willReturn(
                        apiResponse
                );


        // when
        String response =
                footballSyncService
                        .saveTeamTransfers(
                                teamApiId
                        );


        // then
        assertThat(response)
                .isEqualTo(
                        "FootballSyncService.saveTeamTransfers"
                );


        assertThat(playerRepository.count())
                .isZero();

        assertThat(teamRepository.count())
                .isZero();

        assertThat(transferRepository.count())
                .isZero();
    }


    /*
     * [신규]
     *
     * 나타내는 상황:
     *
     * 첫 번째 선수 Bukayo Saka와
     * 관련 Team이 이미 DB에 존재한다.
     *
     * API에서는 동일한 Saka와
     * 새로운 선수 Odegaard를 반환한다.
     *
     * 기대 결과:
     *
     * 기존 Player / Team을 중복 생성하지 않고
     * 필요한 Transfer와 새로운 선수만 저장해야 한다.
     */
    @Test
    @DisplayName(
            "기존 Player와 Team은 재사용하고 새로운 데이터만 저장한다."
    )
    void saveTeamTransfers_existingPlayerAndTeam_reuse() {

        // given
        Player existingPlayer =
                playerRepository.save(
                        Player.of(
                                "Bukayo Saka",
                                10L
                        )
                );


        Team arsenal =
                teamRepository.save(
                        Team.of(
                                "Arsenal",
                                42L
                        )
                );


        Team chelsea =
                teamRepository.save(
                        Team.of(
                                "Chelsea",
                                49L
                        )
                );


        JsonNode apiResponse =
                teamTransfersResponse();


        given(
                footballRestClient
                        .callExternalTeamTransfersApi(
                                42L
                        )
        )
                .willReturn(
                        apiResponse
                );


        // when
        footballSyncService
                .saveTeamTransfers(
                        42L
                );


        // then
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
                        existingPlayer.getId()
                );

        assertThat(foundArsenal.getId())
                .isEqualTo(
                        arsenal.getId()
                );

        assertThat(foundChelsea.getId())
                .isEqualTo(
                        chelsea.getId()
                );
    }


    // ==================================================
    // 예외 상황
    // ==================================================


    /*
     * [신규]
     *
     * 나타내는 상황:
     *
     * API-Football의 팀 이적 API 호출 자체가 실패한다.
     *
     * 기대 결과:
     *
     * 아직 어떤 FootballSyncTxService도 호출되지 않았으므로
     * DB에는 아무런 변경도 발생하지 않아야 한다.
     */
    @Test
    @DisplayName(
            "팀 이적 API 호출에 실패하면 DB 데이터는 변경되지 않는다."
    )
    void saveTeamTransfers_externalApiFail_noDatabaseChange() {

        // given
        Long teamApiId = 42L;


        given(
                footballRestClient
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
                footballSyncService
                        .saveTeamTransfers(
                                teamApiId
                        )
        )
                .isInstanceOf(
                        RuntimeException.class
                )
                .hasMessage(
                        "API-Football 호출 실패"
                );


        assertThat(playerRepository.count())
                .isZero();

        assertThat(teamRepository.count())
                .isZero();

        assertThat(transferRepository.count())
                .isZero();
    }


    /*
     * [신규 - 중요]
     *
     * 나타내는 상황:
     *
     * 팀 이적 API가 세 선수의 데이터를 반환한다.
     *
     * 1. Bukayo Saka
     *      → 정상 데이터
     *      → Transaction 성공 / COMMIT
     *
     * 2. Martin Odegaard
     *      → invalid-date
     *      → Transaction 실패 / ROLLBACK
     *
     * 3. Declan Rice
     *      → 정상 데이터
     *      → 하지만 두 번째 선수에서 예외가 밖으로 전파되므로
     *        현재 구현에서는 처리 자체가 시작되지 않는다.
     *
     * 기대 결과:
     *
     * 첫 번째 선수의 데이터는 유지된다.
     *
     * 두 번째 선수의 Player / Transfer는
     * 해당 Transaction과 함께 Rollback된다.
     *
     * 세 번째 선수는 처리되지 않는다.
     *
     * 이 테스트는 향후
     *
     * "실패한 선수만 기록하고 다음 선수는 계속 처리"
     *
     * 구조로 리팩토링할 때
     * 현재 동작과 변경된 정책을 비교할 기준이 된다.
     */
    @Test
    @DisplayName(
            "중간 선수의 이적 저장이 실패하면 이전 선수는 유지되고 이후 선수는 처리되지 않는다."
    )
    void saveTeamTransfers_secondPlayerFail_partialCommitAndStop() {

        // given
        Long teamApiId = 42L;


        JsonNode apiResponse =
                teamTransfersSecondPlayerInvalidResponse();


        given(
                footballRestClient
                        .callExternalTeamTransfersApi(
                                teamApiId
                        )
        )
                .willReturn(
                        apiResponse
                );


        // when & then
        assertThatThrownBy(() ->
                footballSyncService
                        .saveTeamTransfers(
                                teamApiId
                        )
        )
                .isInstanceOf(
                        DateTimeParseException.class
                );


        /*
         * 첫 번째 선수 Bukayo Saka의 Transaction은
         * 이미 COMMIT되었다.
         */
        assertThat(
                playerRepository
                        .findByApiFootballId(
                                10L
                        )
        )
                .isPresent();


        /*
         * 두 번째 선수 Odegaard는
         * getOrCreatePlayer()까지 실행되더라도
         * 같은 Transaction 안에서 날짜 Parsing 실패가 발생하므로
         * Player 저장까지 Rollback되어야 한다.
         */
        assertThat(
                playerRepository
                        .findByApiFootballId(
                                20L
                        )
        )
                .isEmpty();


        /*
         * 세 번째 선수 Rice는
         * 두 번째 선수의 예외로 for문이 중단되었기 때문에
         * 처리 자체가 시작되지 않는다.
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
         * 첫 번째 선수의 Arsenal / Chelsea는
         * 이미 함께 COMMIT되었다.
         */
        assertThat(teamRepository.count())
                .isEqualTo(2);


        /*
         * 첫 번째 선수의 Transfer만 남는다.
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


        assertThat(
                sakaTransfers
                        .get(0)
                        .getTransferDate()
        )
                .isEqualTo(
                        LocalDate.of(
                                2026,
                                7,
                                1
                        )
                );
    }


    // ==================================================
    // Fixture
    // ==================================================


    /*
     * [Fixture - 정상 팀 이적 목록]
     *
     * 사용 대상:
     *
     * FootballSyncService.saveTeamTransfers()
     *
     * 나타내는 상황:
     *
     * API-Football의 팀 이적 조회 결과에서
     * 두 선수의 이적 정보를 정상적으로 반환한 상황.
     *
     * 선수 1:
     *
     * Bukayo Saka
     * Chelsea -> Arsenal
     * 2026-07-01
     *
     * 선수 2:
     *
     * Martin Odegaard
     * Chelsea -> Arsenal
     * 2025-07-01
     *
     * 이 Fixture는 다음 상황을 검증한다.
     *
     * - 정상 저장
     * - 외부 API 호출 시 Transaction 비활성
     * - 동일 데이터 반복 Sync / 멱등성
     * - 기존 Player / Team 재사용
     */
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


    /*
     * [Fixture - 이적 데이터가 없는 팀 응답]
     *
     * 사용 대상:
     *
     * saveTeamTransfers_emptyResponse_noDatabaseChange()
     *
     * 나타내는 상황:
     *
     * API-Football 호출 자체는 성공했지만
     * response 배열에 처리할 선수 데이터가 없는 상황.
     *
     * 기대되는 동작:
     *
     * for문이 한 번도 실행되지 않으므로
     * Player / Team / Transfer가 생성되지 않는다.
     */
    private JsonNode teamTransfersEmptyResponse() {

        return readJson(
                """
                {
                  "response": []
                }
                """
        );
    }


    /*
     * [Fixture - 두 번째 선수의 이적 날짜가 잘못된 팀 이적 응답]
     *
     * 사용 대상:
     *
     * saveTeamTransfers_secondPlayerFail_partialCommitAndStop()
     *
     * 나타내는 상황:
     *
     * 첫 번째 선수 Bukayo Saka는 정상적으로 저장된다.
     *
     * 두 번째 선수 Martin Odegaard는
     * 이적 날짜가 "invalid-date"이므로
     * LocalDate.parse()에서 실패한다.
     *
     * 세 번째 선수 Declan Rice는 정상 데이터지만
     * 두 번째 선수의 예외 때문에
     * 현재 구현에서는 처리되지 않는다.
     *
     * 이 Fixture를 통해
     *
     * - 선수 단위 Transaction
     * - 이전 선수 Partial Commit
     * - 현재 for문 예외 전파 정책
     *
     * 을 동시에 검증한다.
     */
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


    /*
     * [Fixture Helper - JSON 변환]
     *
     * 문자열로 작성한 API-Football Fixture를
     * JsonNode로 변환한다.
     */
    private JsonNode readJson(
            String json
    ) {

        try {

            return objectMapper
                    .readTree(
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