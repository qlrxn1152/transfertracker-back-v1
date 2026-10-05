package com.dhoon.transfertracker.external.football.service;

import com.dhoon.transfertracker.external.football.client.ApiFootballHttpClient;
import com.dhoon.transfertracker.external.football.dto.TransferSaveResponseDto;
import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
 * @DataJpaTest는 기본적으로 각각의 Test를 Transaction으로 감싼다.
 *
 * 그런데 이번 테스트에서는
 *
 *      FootballSyncService
 *          → Transaction X
 *
 *      FootballSyncTxService
 *          → Transaction O
 *
 * 라는 실제 Transaction 경계를 검증해야 한다.
 *
 * 따라서 테스트 자체의 Transaction은 비활성화한다.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FootballSyncServiceTest {


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
     * 외부 API는 실제로 호출하지 않는다.
     *
     * 이유:
     *
     * 1. 테스트가 API-Football 상태에 의존하면 안 됨
     * 2. API 사용량을 소비하면 안 됨
     * 3. 원하는 응답 / 실패 상황을 직접 만들어야 함
     */
    @MockitoBean
    ApiFootballHttpClient footballRestClient;


    private final ObjectMapper objectMapper =
            new ObjectMapper();


    @BeforeEach
    void clearDatabase() {

        /*
         * Test 자체가 Transaction으로 묶여 있지 않으므로
         * 테스트 사이의 데이터가 남을 수 있다.
         *
         * FK 순서에 맞게 삭제한다.
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
     * 가장 기본적인 정상 동작.
     *
     * 외부 API 응답
     *
     * Bukayo Saka
     * Chelsea -> Arsenal
     *
     * 을 받아서
     *
     * Player
     * Team
     * Transfer
     *
     * 가 모두 저장되는지 검증한다.
     */
    @Test
    @DisplayName(
            "선수 이적 정보를 동기화하면 선수, 팀, 이적 정보가 저장된다."
    )
    void syncPlayerTransfers_success() {

        // given
        Long playerApiId = 10L;

        JsonNode apiResponse =
                playerTransfersResponse(
                        "2026-07-01"
                );

        given(
                footballRestClient
                        .callExternalPlayerTransferApi(
                                playerApiId
                        )
        )
                .willReturn(apiResponse);


        // when
        TransferSaveResponseDto response =
                footballSyncService
                        .syncPlayerTransfers(
                                playerApiId
                        );


        // then
        assertThat(response.getPlayerName())
                .isEqualTo(
                        "Bukayo Saka"
                );

        assertThat(response.getPlayerAPIId())
                .isEqualTo(
                        10L
                );


        assertThat(playerRepository.count())
                .isEqualTo(1);

        assertThat(teamRepository.count())
                .isEqualTo(2);

        assertThat(transferRepository.count())
                .isEqualTo(1);


        Player player =
                playerRepository
                        .findByApiFootballId(
                                10L
                        )
                        .orElseThrow();


        List<Transfer> transfers =
                transferRepository
                        .findAllByPlayerId(
                                player.getId()
                        );


        assertThat(transfers)
                .hasSize(1);


        Transfer transfer =
                transfers.get(0);


        assertThat(transfer.getInTeam().getTeamName())
                .isEqualTo(
                        "Arsenal"
                );

        assertThat(transfer.getOutTeam().getTeamName())
                .isEqualTo(
                        "Chelsea"
                );

        assertThat(transfer.getTransferType())
                .isEqualTo(
                        "Transfer"
                );

        assertThat(transfer.getTransferDate())
                .isEqualTo(
                        LocalDate.of(
                                2026,
                                7,
                                1
                        )
                );
    }


    /*
     * [신규 - 중요]
     *
     * 이번 리팩토링의 핵심 테스트.
     *
     * FootballSyncService는 Transaction을 가지면 안 된다.
     *
     * 따라서 외부 HTTP API를 호출하는 순간에는
     * DB Transaction이 활성화되어 있지 않아야 한다.
     */
    @Test
    @DisplayName(
            "API-Football을 호출하는 동안에는 DB 트랜잭션이 활성화되어 있지 않다."
    )
    void syncPlayerTransfers_externalApiCall_withoutTransaction() {

        // given
        Long playerApiId = 10L;

        JsonNode apiResponse =
                playerTransfersResponse(
                        "2026-07-01"
                );


        given(
                footballRestClient
                        .callExternalPlayerTransferApi(
                                playerApiId
                        )
        )
                .willAnswer(invocation -> {

                    /*
                     * 여기에서 Transaction이 true라면
                     *
                     * 외부 API 응답을 기다리는 동안에도
                     * DB Transaction을 잡고 있다는 의미.
                     */
                    assertThat(
                            TransactionSynchronizationManager
                                    .isActualTransactionActive()
                    )
                            .isFalse();


                    return apiResponse;
                });


        // when
        footballSyncService
                .syncPlayerTransfers(
                        playerApiId
                );


        // then
        then(footballRestClient)
                .should(times(1))
                .callExternalPlayerTransferApi(
                        playerApiId
                );
    }


    /*
     * [신규 - 중요]
     *
     * 같은 API 데이터를 여러 번 가져올 수 있다.
     *
     * Scheduler를 붙이게 되면 특히 중요하다.
     *
     * 같은 동기화를 두 번 실행하더라도
     * 동일 Transfer는 한 건만 존재해야 한다.
     */
    @Test
    @DisplayName(
            "동일한 이적 정보를 여러 번 동기화해도 중복 저장하지 않는다."
    )
    void syncPlayerTransfers_sameTransfer_idempotent() {

        // given
        Long playerApiId = 10L;

        JsonNode apiResponse =
                playerTransfersResponse(
                        "2026-07-01"
                );


        given(
                footballRestClient
                        .callExternalPlayerTransferApi(
                                playerApiId
                        )
        )
                .willReturn(
                        apiResponse
                );


        // when
        footballSyncService
                .syncPlayerTransfers(
                        playerApiId
                );

        footballSyncService
                .syncPlayerTransfers(
                        playerApiId
                );


        // then
        assertThat(playerRepository.count())
                .isEqualTo(1);

        assertThat(teamRepository.count())
                .isEqualTo(2);

        assertThat(transferRepository.count())
                .isEqualTo(1);


        then(footballRestClient)
                .should(times(2))
                .callExternalPlayerTransferApi(
                        playerApiId
                );
    }


    /*
     * [신규 - 중요]
     *
     * Transfer Business Key:
     *
     * player
     * + inTeam
     * + outTeam
     * + transferDate
     *
     * 따라서 같은 선수가
     * 같은 두 팀 사이에서 이동했더라도
     * 날짜가 다르면 서로 다른 Transfer다.
     */
    @Test
    @DisplayName(
            "선수와 팀이 같더라도 이적 날짜가 다르면 별도의 이적 정보로 저장한다."
    )
    void syncPlayerTransfers_sameTeamsDifferentDate_saveSeparately() {

        // given
        Long playerApiId = 10L;


        JsonNode apiResponse =
                playerTransfersResponse(
                        "2022-07-01",
                        "2026-07-01"
                );


        given(
                footballRestClient
                        .callExternalPlayerTransferApi(
                                playerApiId
                        )
        )
                .willReturn(
                        apiResponse
                );


        // when
        footballSyncService
                .syncPlayerTransfers(
                        playerApiId
                );


        // then
        assertThat(transferRepository.count())
                .isEqualTo(2);


        Player player =
                playerRepository
                        .findByApiFootballId(
                                playerApiId
                        )
                        .orElseThrow();


        List<Transfer> transfers =
                transferRepository
                        .findAllByPlayerId(
                                player.getId()
                        );


        assertThat(transfers)
                .extracting(
                        Transfer::getTransferDate
                )
                .containsExactly(
                        LocalDate.of(
                                2026,
                                7,
                                1
                        ),
                        LocalDate.of(
                                2022,
                                7,
                                1
                        )
                );
    }


    /*
     * [신규]
     *
     * DB에 이미 선수와 팀이 존재하는 경우
     * 중복 Entity를 생성하면 안 된다.
     */
    @Test
    @DisplayName(
            "이미 존재하는 선수와 팀은 새로 저장하지 않고 기존 데이터를 사용한다."
    )
    void syncPlayerTransfers_existingPlayerAndTeam_reuse() {

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
                playerTransfersResponse(
                        "2026-07-01"
                );


        given(
                footballRestClient
                        .callExternalPlayerTransferApi(
                                10L
                        )
        )
                .willReturn(
                        apiResponse
                );


        // when
        footballSyncService
                .syncPlayerTransfers(
                        10L
                );


        // then
        assertThat(playerRepository.count())
                .isEqualTo(1);

        assertThat(teamRepository.count())
                .isEqualTo(2);

        assertThat(transferRepository.count())
                .isEqualTo(1);


        Player foundPlayer =
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


        assertThat(foundPlayer.getId())
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


    /*
     * [신규 - 중요]
     *
     * syncLeagueTeams() 리팩토링 검증.
     *
     * assignTeamLeague()가 Transaction 밖에서 실행되면
     * Dirty Checking이 동작하지 않을 수 있다.
     *
     * DB를 다시 조회했을 때 leagueCode가 존재하는지 검증한다.
     */
    @Test
    @DisplayName(
            "리그의 팀을 동기화하면 해당 팀의 leagueCode가 DB에 반영된다."
    )
    void syncLeagueTeams_assignLeagueCode() {

        // given
        JsonNode apiResponse =
                leagueTeamsResponse();


        given(
                footballRestClient
                        .callExternalLeagueTeamsApi(
                                LeagueCode.EPL
                        )
        )
                .willReturn(
                        apiResponse
                );


        // when
        footballSyncService
                .syncLeagueTeams(
                        LeagueCode.EPL
                );


        // then
        Team arsenal =
                teamRepository
                        .findByApiFootballId(
                                42L
                        )
                        .orElseThrow();


        Team chelsea =
                teamRepository
                        .findByApiFootballId(
                                49L
                        )
                        .orElseThrow();


        assertThat(arsenal.getLeagueCode())
                .isEqualTo(
                        LeagueCode.EPL
                );

        assertThat(chelsea.getLeagueCode())
                .isEqualTo(
                        LeagueCode.EPL
                );
    }


    // ==================================================
    // 예외 상황
    // ==================================================


    /*
     * [신규 - 중요]
     *
     * 하나의 FootballSyncTxService Transaction 안에서
     *
     * 1번째 Transfer → 정상 저장
     * 2번째 Transfer → 날짜 Parsing 실패
     *
     * 가 발생하도록 만든다.
     *
     * Transaction이 정상적으로 적용되어 있다면
     * 첫 번째 저장까지 모두 Rollback되어야 한다.
     */
    @Test
    @DisplayName(
            "이적 정보 저장 중 예외가 발생하면 선수, 팀, 이적 정보를 모두 롤백한다."
    )
    void syncPlayerTransfers_fail_rollbackAll() {

        // given
        Long playerApiId = 10L;


        JsonNode apiResponse =
                playerTransfersResponse(
                        "2026-07-01",
                        "invalid-date"
                );


        given(
                footballRestClient
                        .callExternalPlayerTransferApi(
                                playerApiId
                        )
        )
                .willReturn(
                        apiResponse
                );


        // when & then
        assertThatThrownBy(() ->
                footballSyncService
                        .syncPlayerTransfers(
                                playerApiId
                        )
        )
                .isInstanceOf(
                        DateTimeParseException.class
                );


        /*
         * 첫 번째 Transfer를 처리하는 과정에서
         *
         * Player
         * Arsenal
         * Chelsea
         * Transfer
         *
         * 가 save() 되었더라도,
         *
         * 두 번째 Transfer에서 예외가 발생했으므로
         * Transaction 전체가 Rollback되어야 한다.
         */
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
     * HTTP 호출 자체가 실패했다면
     * 아직 FootballSyncTxService에 진입하기 전이다.
     *
     * 따라서 DB 변경은 전혀 없어야 한다.
     */
    @Test
    @DisplayName(
            "API-Football 호출에 실패하면 DB 데이터는 변경되지 않는다."
    )
    void syncPlayerTransfers_externalApiFail_noDatabaseChange() {

        // given
        Long playerApiId = 10L;


        given(
                footballRestClient
                        .callExternalPlayerTransferApi(
                                playerApiId
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
                        .syncPlayerTransfers(
                                playerApiId
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


    // ==================================================
    // Fixture
    // ==================================================


    /*
     * API-Football
     *
     * GET /transfers?player=10
     *
     * 형태의 응답을 테스트용으로 생성한다.
     */
    private JsonNode playerTransfersResponse(
            String... transferDates
    ) {

        String transfers =
                Arrays.stream(
                                transferDates
                        )
                        .map(date ->
                                """
                                {
                                  "date": "%s",
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
                                """
                                        .formatted(
                                                date
                                        )
                        )
                        .collect(
                                Collectors.joining(",")
                        );


        String json =
                """
                {
                  "response": [
                    {
                      "player": {
                        "id": 10,
                        "name": "Bukayo Saka"
                      },
                      "transfers": [
                        %s
                      ]
                    }
                  ]
                }
                """
                        .formatted(
                                transfers
                        );


        return readJson(
                json
        );
    }


    /*
     * API-Football
     *
     * GET /teams?league=...
     *
     * 응답 구조.
     *
     * 중요한 부분은 response 바로 아래가 Team이 아니라
     *
     * response
     *   └ team
     *
     * 구조라는 점.
     */
    private JsonNode leagueTeamsResponse() {

        return readJson(
                """
                {
                  "response": [
                    {
                      "team": {
                        "id": 42,
                        "name": "Arsenal"
                      }
                    },
                    {
                      "team": {
                        "id": 49,
                        "name": "Chelsea"
                      }
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