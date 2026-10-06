package com.dhoon.transfertracker.external.football.service;

import com.dhoon.transfertracker.external.football.client.ApiFootballHttpClient;
import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.transfer.domain.Transfer;
import com.dhoon.transfertracker.internal.transfer.dto.response.PlayerTransferItemResponseDto;
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
 * [기존 유지 - 중요]
 *
 * @DataJpaTest는 기본적으로 테스트 자체를
 * Transaction으로 감싼다.
 *
 * 하지만 실제 Football Sync 구조는
 *
 * FootballSyncService
 *      → 외부 API 호출
 *      → Transaction X
 *
 * FootballSyncTxService
 *      → Player 조회 / 생성
 *      → Player의 전체 Transfer 처리
 *      → DTO 생성
 *      → Transaction O
 *
 * 구조다.
 *
 * 실제 Transaction 경계를 검증하기 위해
 * 테스트 자체의 Transaction은 비활성화한다.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FootballSyncPlayerTransfersServiceTest {


    @Autowired
    FootballSyncService footballSyncService;


    @Autowired
    PlayerRepository playerRepository;


    @Autowired
    TeamRepository teamRepository;


    @Autowired
    TransferRepository transferRepository;


    /*
     * 실제 API-Football 서버를 호출하지 않는다.
     *
     * 테스트에서
     *
     * - 정상 응답
     * - 복수 이적
     * - 잘못된 이적 데이터
     * - 외부 API 실패
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
         * Transfer가 Player / Team을 FK로 참조하기 때문에
         * Transfer부터 삭제한다.
         */
        transferRepository.deleteAll();

        playerRepository.deleteAll();
        teamRepository.deleteAll();
    }


    // ==================================================
    // 정상 상황
    // ==================================================


    /*
     * [기존 수정]
     *
     * 가장 기본적인 syncPlayerTransfers() 정상 동작.
     *
     *
     * 상황:
     *
     * API-Football에서
     *
     * Bukayo Saka
     *
     * Chelsea
     *      ↓
     * Arsenal
     *
     * 2026-07-01
     *
     * 이적 정보를 반환한다.
     *
     *
     * 현재 리팩토링 구조:
     *
     * FootballSyncService
     *
     *      ↓
     *
     * 외부 API 호출
     *
     *      ↓
     *
     * FootballSyncTxService
     *
     *      ↓
     *
     * Player 조회 / 생성 1회
     *
     *      ↓
     *
     * 모든 Transfer 처리
     *
     *      ↓
     *
     * PlayerTransfersResponseDto 생성
     *
     *      ↓
     *
     * Commit
     *
     *
     * 기대 결과:
     *
     * Player   1건
     * Team     2건
     * Transfer 1건
     *
     * +
     *
     * 정상 DTO 반환
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
                .willReturn(
                        apiResponse
                );


        // when
        PlayerTransfersResponseDto response =
                footballSyncService
                        .syncPlayerTransfers(
                                playerApiId
                        );


        // then

        /*
         * 현재 PlayerTransfersResponseDto 구조:
         *
         * playerName
         * playerTransfers
         */
        assertThat(response.getPlayerName())
                .isEqualTo(
                        "Bukayo Saka"
                );


        assertThat(response.getPlayerTransfers())
                .hasSize(1);


        PlayerTransferItemResponseDto transferResponse =
                response.getPlayerTransfers()
                        .get(0);


        assertThat(transferResponse.getInTeamName())
                .isEqualTo(
                        "Arsenal"
                );


        assertThat(transferResponse.getOutTeamName())
                .isEqualTo(
                        "Chelsea"
                );


        assertThat(transferResponse.getDate())
                .isEqualTo(
                        LocalDate.of(
                                2026,
                                7,
                                1
                        )
                );


        assertThat(transferResponse.getType())
                .isEqualTo(
                        "Transfer"
                );


        /*
         * DTO만 정상인 것이 아니라
         * 실제 DB 상태도 검증한다.
         */
        assertThat(playerRepository.count())
                .isEqualTo(1);


        assertThat(teamRepository.count())
                .isEqualTo(2);


        assertThat(transferRepository.count())
                .isEqualTo(1);


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
     * [기존 유지 - 중요]
     *
     * 외부 API 호출 동안에는
     * DB Transaction이 활성화되어 있으면 안 된다.
     *
     * Transaction은 외부 응답을 받은 뒤
     * FootballSyncTxService에 진입하면서 시작한다.
     */
    @Test
    @DisplayName(
            "선수 이적 API를 호출하는 동안에는 DB 트랜잭션이 활성화되어 있지 않다."
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
                     * true라면 외부 API 응답을 기다리는 동안에도
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


        assertThat(playerRepository.count())
                .isEqualTo(1);


        assertThat(teamRepository.count())
                .isEqualTo(2);


        assertThat(transferRepository.count())
                .isEqualTo(1);
    }


    /*
     * [기존 유지 - 중요]
     *
     * 동일한 Transfer Sync가 여러 번 발생할 수 있다.
     *
     * 향후 Scheduler가 실행되면
     * 같은 과거 이적 데이터를 다시 받을 가능성이 높다.
     *
     *
     * 기대 결과:
     *
     * API 호출 = 2번
     *
     * 하지만
     *
     * Player   = 1
     * Team     = 2
     * Transfer = 1
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
     * [기존 유지 - 중요]
     *
     * Transfer 중복 판단 기준:
     *
     * Player
     * + InTeam
     * + OutTeam
     * + TransferDate
     *
     *
     * 따라서 같은 선수가
     * 같은 두 팀 사이에서 이동했더라도
     *
     * 날짜가 다르면 별개의 Transfer다.
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
        PlayerTransfersResponseDto response =
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
                .isEqualTo(2);


        /*
         * 응답 DTO에도 두 Transfer가 존재해야 한다.
         */
        assertThat(response.getPlayerTransfers())
                .hasSize(2);


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
     * [기존 유지]
     *
     * 상황:
     *
     * DB에 이미
     *
     * Bukayo Saka
     * Arsenal
     * Chelsea
     *
     * 가 존재한다.
     *
     *
     * 기대 결과:
     *
     * 기존 Entity를 재사용하고
     * Transfer만 생성해야 한다.
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
        PlayerTransfersResponseDto response =
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


        /*
         * 기존 Entity가 실제로 재사용됐는지
         * DB PK로 확인한다.
         */
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


        /*
         * 기존 Entity를 사용한 상황에서도
         * Tx 내부 DTO 생성은 정상이어야 한다.
         */
        assertThat(response.getPlayerName())
                .isEqualTo(
                        "Bukayo Saka"
                );


        assertThat(response.getPlayerTransfers())
                .hasSize(1);
    }


    /*
     * [신규 - 경계]
     *
     * API 응답에는 Player가 존재하지만
     * transfers 배열이 비어있는 상황.
     *
     *
     * 현재 구현:
     *
     * getOrCreatePlayerTransfersResponse()
     *
     *      ↓
     *
     * Player는 먼저 조회 / 생성
     *
     *      ↓
     *
     * transferDatas 반복
     *
     *      ↓
     *
     * 반복 횟수 0
     *
     *
     * 따라서:
     *
     * Player   = 1
     * Team     = 0
     * Transfer = 0
     *
     * 이 현재 구현의 동작이다.
     */
    @Test
    @DisplayName(
            "이적 목록이 비어있으면 Player만 저장하고 빈 이적 목록을 반환한다."
    )
    void syncPlayerTransfers_emptyTransfers_savePlayerOnly() {

        // given
        Long playerApiId = 10L;


        JsonNode apiResponse =
                playerTransfersResponse();


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
        PlayerTransfersResponseDto response =
                footballSyncService
                        .syncPlayerTransfers(
                                playerApiId
                        );


        // then
        assertThat(playerRepository.count())
                .isEqualTo(1);


        assertThat(teamRepository.count())
                .isZero();


        assertThat(transferRepository.count())
                .isZero();


        assertThat(response.getPlayerName())
                .isEqualTo(
                        "Bukayo Saka"
                );


        assertThat(response.getPlayerTransfers())
                .isEmpty();
    }


    // ==================================================
    // 예외 상황
    // ==================================================


    /*
     * [기존 유지 - 핵심]
     *
     * 이번 PlayerTransfers 리팩토링에서
     * 가장 중요한 Transaction 테스트.
     *
     *
     * 한 선수의 전체 이적 목록은
     * 하나의 Transaction 단위다.
     *
     *
     * 상황:
     *
     * Transfer #1
     *      → 2026-07-01
     *      → 정상
     *
     * Transfer #2
     *      → invalid-date
     *      → 날짜 Parsing 실패
     *
     *
     * Transfer #1 처리 중 이미
     *
     * Player
     * Arsenal
     * Chelsea
     * Transfer
     *
     * 가 save 되었더라도
     *
     * Transfer #2에서 RuntimeException이 발생하면
     * 전체 Transaction이 Rollback되어야 한다.
     */
    @Test
    @DisplayName(
            "이적 정보 저장 중 예외가 발생하면 해당 선수의 전체 이적 저장을 롤백한다."
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
         * 한 Player의 Transfer 전체가
         * 하나의 Transaction이므로
         * 모두 Rollback되어야 한다.
         */
        assertThat(playerRepository.count())
                .isZero();


        assertThat(teamRepository.count())
                .isZero();


        assertThat(transferRepository.count())
                .isZero();
    }


    /*
     * [기존 유지]
     *
     * 외부 API 호출 자체가 실패하는 경우.
     *
     * 아직 FootballSyncTxService에 진입하지 않았으므로
     * DB Transaction도 시작되지 않는다.
     */
    @Test
    @DisplayName(
            "선수 이적 API 호출에 실패하면 DB 데이터는 변경되지 않는다."
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


        then(footballRestClient)
                .should(times(1))
                .callExternalPlayerTransferApi(
                        playerApiId
                );
    }


    // ==================================================
    // Fixture
    // ==================================================


    /*
     * [Fixture - 선수 이적 목록]
     *
     * 사용 대상:
     *
     * FootballSyncService.syncPlayerTransfers()
     *
     *
     * API-Football 형태:
     *
     * response
     *   └ player
     *   └ transfers[]
     *
     *
     * transferDates를 가변 인자로 받아서
     *
     * - 단일 Transfer
     * - 복수 Transfer
     * - 빈 Transfer
     * - 잘못된 날짜
     *
     * 상황을 모두 만들 수 있다.
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