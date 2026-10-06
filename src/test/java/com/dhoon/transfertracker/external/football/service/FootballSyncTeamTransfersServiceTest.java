package com.dhoon.transfertracker.external.football.service;

import com.dhoon.transfertracker.external.football.client.ApiFootballHttpClient;
import com.dhoon.transfertracker.external.football.dto.TeamTransfersSaveResponseDto;
import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.exception.NotFoundTeamException;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.teamplayer.repository.TeamPlayerRepository;
import com.dhoon.transfertracker.internal.transfer.domain.Transfer;
import com.dhoon.transfertracker.internal.transfer.dto.response.PlayerTransfersResponseDto;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;


@DataJpaTest
@Import({
        FootballSyncService.class,
        FootballSyncTxService.class
})
@ActiveProfiles("test")

/*
 * [기존 수정 - 중요]
 *
 * @DataJpaTest는 기본적으로 각각의 테스트를
 * Transaction으로 감싼다.
 *
 * 하지만 실제 saveTeamTransfers() 구조는:
 *
 * FootballSyncService
 *
 *      ↓
 *
 * FootballSyncTxService.getDisplayTeamName()
 *      → Team 존재 확인
 *      → readOnly Transaction
 *
 *      ↓
 *
 * Transaction 종료
 *
 *      ↓
 *
 * API-Football Team Transfers 호출
 *      → Transaction X
 *
 *      ↓
 *
 * response 하나 = Player 한 명
 *
 *      ↓
 *
 * FootballSyncTxService
 * .getOrCreatePlayerTransfersResponse()
 *
 *      → Player 한 명의 전체 Transfer 처리
 *      → Transaction O
 *
 *
 * 따라서:
 *
 * Player #1 = Tx #1
 * Player #2 = Tx #2
 * Player #3 = Tx #3
 *
 * 구조를 실제와 동일하게 검증하기 위해
 * Test 자체 Transaction을 비활성화한다.
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
     * 실제 API-Football은 호출하지 않는다.
     *
     * 테스트에서:
     *
     * - 정상 응답
     * - 빈 응답
     * - API 실패
     * - 중간 Player 실패
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
         * Test Transaction을 꺼두었기 때문에
         * 테스트 종료 후 자동 Rollback되지 않는다.
         *
         * 따라서 FK 자식 Entity부터 직접 삭제한다.
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
     * [기존 수정 - 중요]
     *
     * 상황:
     *
     * Arsenal은 이미 우리 DB에 존재한다.
     *
     * 이후 Arsenal Team Transfers Sync를 실행한다.
     *
     * API-Football에서는:
     *
     * Bukayo Saka
     * Martin Odegaard
     *
     * 두 선수의 이적정보를 반환한다.
     *
     *
     * 기대 결과:
     *
     * 기존 Arsenal 재사용
     * Chelsea 신규 생성
     *
     * Player   = 2
     * Team     = 2
     * Transfer = 2
     *
     * 그리고 TeamTransfersSaveResponseDto가
     * 정상적으로 반환되어야 한다.
     */
    @Test
    @DisplayName(
            "팀 이적 정보를 동기화하면 여러 선수의 이적정보를 저장하고 DTO를 반환한다."
    )
    void saveTeamTransfers_success() {

        // given
        Long teamApiId = 42L;


        Team arsenal =
                teamRepository.save(
                        Team.of(
                                "Arsenal",
                                teamApiId
                        )
                );


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
        TeamTransfersSaveResponseDto response =
                footballSyncService
                        .saveTeamTransfers(
                                teamApiId
                        );


        // then

        /*
         * Team 이름은 외부 Team API를 추가 호출해서 얻는 것이 아니라
         * 기존 DB Team에서 가져온다.
         */
        assertThat(response.getTeamName())
                .isEqualTo(
                        "Arsenal"
                );


        assertThat(response.getPlayersTransfers())
                .hasSize(2);


        assertThat(response.getPlayersTransfers())
                .extracting(
                        PlayerTransfersResponseDto::getPlayerName
                )
                .containsExactly(
                        "Bukayo Saka",
                        "Martin Odegaard"
                );


        /*
         * Saka 응답 확인.
         */
        PlayerTransfersResponseDto sakaResponse =
                response
                        .getPlayersTransfers()
                        .get(0);


        assertThat(sakaResponse.getPlayerTransfers())
                .hasSize(1);


        assertThat(
                sakaResponse
                        .getPlayerTransfers()
                        .get(0)
                        .getInTeamName()
        )
                .isEqualTo(
                        "Arsenal"
                );


        assertThat(
                sakaResponse
                        .getPlayerTransfers()
                        .get(0)
                        .getOutTeamName()
        )
                .isEqualTo(
                        "Chelsea"
                );


        assertThat(
                sakaResponse
                        .getPlayerTransfers()
                        .get(0)
                        .getDate()
        )
                .isEqualTo(
                        LocalDate.of(
                                2026,
                                7,
                                1
                        )
                );


        /*
         * 실제 DB 검증.
         */
        assertThat(playerRepository.count())
                .isEqualTo(2);


        assertThat(teamRepository.count())
                .isEqualTo(2);


        assertThat(transferRepository.count())
                .isEqualTo(2);


        /*
         * 기존 Arsenal Entity가 재사용됐는지도 확인한다.
         */
        Team foundArsenal =
                teamRepository
                        .findByApiFootballId(
                                42L
                        )
                        .orElseThrow();


        assertThat(foundArsenal.getId())
                .isEqualTo(
                        arsenal.getId()
                );


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
     * [기존 수정 - 중요]
     *
     * 흐름:
     *
     * DB Team 조회
     *      ↓
     * readOnly Tx 종료
     *      ↓
     * 외부 API 호출
     *
     * 따라서 외부 API 호출 시점에는
     * Transaction이 없어야 한다.
     */
    @Test
    @DisplayName(
            "팀 이적 API를 호출하는 동안에는 DB 트랜잭션이 활성화되어 있지 않다."
    )
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
                footballRestClient
                        .callExternalTeamTransfersApi(
                                teamApiId
                        )
        )
                .willAnswer(invocation -> {

                    /*
                     * getDisplayTeamName()의 readOnly Transaction은
                     * 이미 끝난 상태여야 한다.
                     */
                    assertThat(
                            TransactionSynchronizationManager
                                    .isActualTransactionActive()
                    )
                            .isFalse();


                    return apiResponse;
                });


        // when
        TeamTransfersSaveResponseDto response =
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


        assertThat(response.getTeamName())
                .isEqualTo(
                        "Arsenal"
                );


        assertThat(response.getPlayersTransfers())
                .hasSize(2);


        assertThat(playerRepository.count())
                .isEqualTo(2);


        assertThat(transferRepository.count())
                .isEqualTo(2);
    }


    /*
     * [기존 수정 - 중요]
     *
     * 동일한 Team Transfers Sync를 두 번 실행한다.
     *
     *
     * 기대 결과:
     *
     * 기존 Player / Team / Transfer를 재사용한다.
     *
     * API 호출은 두 번 발생하지만
     * 데이터 건수는 증가하지 않는다.
     */
    @Test
    @DisplayName(
            "동일한 팀 이적 정보를 여러 번 동기화해도 중복 저장하지 않는다."
    )
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
                footballRestClient
                        .callExternalTeamTransfersApi(
                                teamApiId
                        )
        )
                .willReturn(
                        apiResponse
                );


        // when
        TeamTransfersSaveResponseDto firstResponse =
                footballSyncService
                        .saveTeamTransfers(
                                teamApiId
                        );


        TeamTransfersSaveResponseDto secondResponse =
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


        assertThat(firstResponse.getPlayersTransfers())
                .hasSize(2);


        assertThat(secondResponse.getPlayersTransfers())
                .hasSize(2);


        assertThat(secondResponse.getTeamName())
                .isEqualTo(
                        "Arsenal"
                );


        then(footballRestClient)
                .should(times(2))
                .callExternalTeamTransfersApi(
                        teamApiId
                );
    }


    /*
     * [기존 수정 - 경계]
     *
     * Team은 DB에 존재한다.
     *
     * 하지만 API-Football의 response 배열은 비어있다.
     *
     *
     * 기대 결과:
     *
     * 기존 Team은 그대로 존재한다.
     *
     * Player / Transfer는 생성되지 않는다.
     *
     * DTO:
     *
     * teamName        = Arsenal
     * playersTransfers = []
     */
    @Test
    @DisplayName(
            "팀 이적 응답이 비어있으면 빈 선수 이적 목록을 반환하고 새로운 데이터를 생성하지 않는다."
    )
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
        TeamTransfersSaveResponseDto response =
                footballSyncService
                        .saveTeamTransfers(
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


        /*
         * Arsenal은 Sync 이전부터 존재했다.
         */
        assertThat(teamRepository.count())
                .isEqualTo(1);


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


        assertThat(transferRepository.count())
                .isZero();
    }


    /*
     * [기존 수정]
     *
     * 기존 Player / Team이 존재할 때
     * 실제 Entity를 재사용하는지 확인한다.
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
        TeamTransfersSaveResponseDto response =
                footballSyncService
                        .saveTeamTransfers(
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
     * [신규 - 중요]
     *
     * TeamTransfers의 현재 전제:
     *
     * 요청 대상 Team은 이미 DB에 존재해야 한다.
     *
     *
     * 따라서 Team이 존재하지 않으면:
     *
     * getDisplayTeamName()
     *
     * 에서 바로 NotFoundTeamException이 발생한다.
     *
     * 중요한 점:
     *
     * Team 존재 확인이 외부 API 호출보다 먼저이므로
     * 불필요한 API-Football 호출도 발생하지 않는다.
     */
    @Test
    @DisplayName(
            "DB에 Team이 존재하지 않으면 외부 API를 호출하지 않고 예외가 발생한다."
    )
    void saveTeamTransfers_notFoundTeam_noExternalApiCall() {

        // given
        Long teamApiId = 42L;


        // when & then
        assertThatThrownBy(() ->
                footballSyncService
                        .saveTeamTransfers(
                                teamApiId
                        )
        )
                .isInstanceOf(
                        NotFoundTeamException.class
                )
                .hasMessage(
                        "존재하지 않는 팀 입니다."
                );


        /*
         * Team 존재 확인 단계에서 실패했으므로
         * 외부 API는 호출되면 안 된다.
         */
        then(footballRestClient)
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
    }


    /*
     * [기존 수정]
     *
     * Team은 DB에 정상적으로 존재하지만
     * 그 다음 외부 API 호출에서 실패하는 상황.
     *
     *
     * 기대 결과:
     *
     * 기존 Team은 그대로 유지된다.
     *
     * Player / Transfer 데이터는 생성되지 않는다.
     */
    @Test
    @DisplayName(
            "팀 이적 API 호출에 실패하면 새로운 DB 데이터는 생성되지 않는다."
    )
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


        then(footballRestClient)
                .should(times(1))
                .callExternalTeamTransfersApi(
                        teamApiId
                );


        assertThat(playerRepository.count())
                .isZero();


        /*
         * 기존 Arsenal만 유지된다.
         */
        assertThat(teamRepository.count())
                .isEqualTo(1);


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


        assertThat(transferRepository.count())
                .isZero();
    }


    /*
     * [기존 수정 - 중요]
     *
     * TeamTransfers의 핵심 Transaction 정책을 검증한다.
     *
     *
     * API 응답:
     *
     * Player #1 - Bukayo Saka
     *      → 정상
     *      → Tx #1 COMMIT
     *
     * Player #2 - Martin Odegaard
     *      → invalid-date
     *      → Tx #2 ROLLBACK
     *
     * Player #3 - Declan Rice
     *      → 정상 데이터
     *      → 하지만 #2 예외로 반복문 중단
     *      → 실행되지 않음
     *
     *
     * 최종 상태:
     *
     * Arsenal        = 기존 Team 유지
     * Chelsea        = #1 Transaction에서 저장
     * Saka           = 저장
     * Saka Transfer  = 저장
     * Odegaard       = Rollback
     * Rice           = 처리되지 않음
     */
    @Test
    @DisplayName(
            "중간 선수의 이적 저장이 실패하면 이전 선수는 유지되고 이후 선수는 처리되지 않는다."
    )
    void saveTeamTransfers_secondPlayerFail_partialCommitAndStop() {

        // given
        Long teamApiId = 42L;


        teamRepository.save(
                Team.of(
                        "Arsenal",
                        teamApiId
                )
        );


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
         * Player #1
         *
         * 이미 자신의 Transaction을 성공적으로 Commit했다.
         */
        assertThat(
                playerRepository
                        .findByApiFootballId(
                                10L
                        )
        )
                .isPresent();


        /*
         * Player #2
         *
         * Player 저장 이후 날짜 Parsing에서 실패하지만
         * 하나의 Transaction 안이므로 Player 저장도 Rollback된다.
         */
        assertThat(
                playerRepository
                        .findByApiFootballId(
                                20L
                        )
        )
                .isEmpty();


        /*
         * Player #3
         *
         * #2의 예외가 FootballSyncService까지 전파되어
         * for문이 종료되므로 처리되지 않는다.
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
         * Arsenal:
         * Sync 실행 전부터 존재.
         *
         * Chelsea:
         * Saka Transaction에서 생성되어 Commit.
         */
        assertThat(teamRepository.count())
                .isEqualTo(2);


        /*
         * Saka의 Transfer 한 건만 Commit되어 있다.
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
     * [Fixture - 정상 Team Transfers]
     *
     * 대상 Team:
     *
     * Arsenal
     *
     *
     * Player #1
     *
     * Bukayo Saka
     * Chelsea -> Arsenal
     * 2026-07-01
     *
     *
     * Player #2
     *
     * Martin Odegaard
     * Chelsea -> Arsenal
     * 2025-07-01
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
     * [Fixture - 선수 이적정보 없음]
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
     * [Fixture - 두 번째 Player 실패]
     *
     * Saka
     *      → 정상
     *
     * Odegaard
     *      → invalid-date
     *
     * Rice
     *      → 정상 데이터지만 처리되지 않음
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
     * [Fixture Helper]
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