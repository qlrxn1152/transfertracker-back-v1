package com.dhoon.transfertracker.external.football.service;

import com.dhoon.transfertracker.external.football.client.ApiFootballHttpClient;
import com.dhoon.transfertracker.external.football.dto.PlayerSaveResponseDto;
import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
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
 * @DataJpaTest는 기본적으로 각각의 테스트 메서드를
 * Transaction으로 감싼다.
 *
 * 하지만 실제 Football Sync 구조는
 *
 * FootballSyncService
 *      → 외부 API 호출
 *      → Transaction X
 *
 * FootballSyncTxService
 *      → DB 작업
 *      → DTO 변환
 *      → Transaction O
 *
 * 이다.
 *
 * 따라서 실제 Transaction 경계를 검증하기 위해
 * 테스트 자체의 Transaction은 비활성화한다.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FootballSyncPlayerServiceTest {


    @Autowired
    FootballSyncService footballSyncService;


    @Autowired
    PlayerRepository playerRepository;


    /*
     * 실제 API-Football 서버는 호출하지 않는다.
     *
     * 테스트에서 원하는
     *
     * - 정상 응답
     * - 예외 상황
     *
     * 을 직접 만들기 위해 Mock 처리한다.
     */
    @MockitoBean
    ApiFootballHttpClient footballRestClient;


    private final ObjectMapper objectMapper =
            new ObjectMapper();


    @BeforeEach
    void clearDatabase() {

        /*
         * 테스트 자체의 Transaction을 꺼두었기 때문에
         * 자동 Rollback에 의존하지 않는다.
         *
         * 각각의 테스트가 독립적으로 실행될 수 있도록
         * Player 데이터를 직접 삭제한다.
         */
        playerRepository.deleteAll();
    }


    // ==================================================
    // 정상 상황
    // ==================================================


    /*
     * [기존 유지]
     *
     * 상황:
     *
     * DB에 Bukayo Saka가 존재하지 않는다.
     *
     * API-Football에서는
     *
     * id   = 10
     * name = Bukayo Saka
     *
     * 를 반환한다.
     *
     *
     * 현재 리팩토링 구조:
     *
     * FootballSyncService.syncPlayer()
     *
     *      ↓
     *
     * API-Football 호출
     *
     *      ↓
     *
     * FootballSyncTxService.getOrCreatePlayerResponse()
     *
     *      ↓
     *
     * getOrCreatePlayer()
     *
     *      ↓
     *
     * Player 조회 / 저장
     *
     *      ↓
     *
     * 같은 Transaction 안에서
     * PlayerSaveResponseDto 생성
     *
     *
     * 기대 결과:
     *
     * Player 1건 저장
     * +
     * 정상 DTO 반환
     */
    @Test
    @DisplayName(
            "선수 정보를 동기화하면 새로운 Player가 저장된다."
    )
    void syncPlayer_success() {

        // given
        Long playerApiId = 10L;


        JsonNode apiResponse =
                playerResponse(
                        10L,
                        "Bukayo Saka"
                );


        given(
                footballRestClient
                        .callExternalPlayerApi(
                                playerApiId
                        )
        )
                .willReturn(
                        apiResponse
                );


        // when
        PlayerSaveResponseDto response =
                footballSyncService
                        .syncPlayer(
                                playerApiId
                        );


        // then

        /*
         * [리팩토링 확인]
         *
         * FootballSyncService가 Player Entity를 받아서
         * DTO로 변환하는 것이 아니다.
         *
         * TxService 안에서 이미 생성된
         * PlayerSaveResponseDto가 반환된다.
         */
        assertThat(response.getPlayerName())
                .isEqualTo(
                        "Bukayo Saka"
                );


        assertThat(response.getPlayerAPIId())
                .isEqualTo(
                        10L
                );


        /*
         * 실제 DB에도 Player가 저장되어야 한다.
         */
        assertThat(playerRepository.count())
                .isEqualTo(1);


        Player player =
                playerRepository
                        .findByApiFootballId(
                                playerApiId
                        )
                        .orElseThrow();


        assertThat(player.getPlayerName())
                .isEqualTo(
                        "Bukayo Saka"
                );


        assertThat(player.getApiFootballId())
                .isEqualTo(
                        10L
                );
    }


    /*
     * [기존 유지 - 중요]
     *
     * 이번 리팩토링에서도 유지되어야 할
     * 가장 중요한 Transaction 경계 중 하나.
     *
     *
     * 상황:
     *
     * FootballSyncService가
     * API-Football에 요청을 보내는 순간.
     *
     *
     * 기대 결과:
     *
     * 외부 HTTP 요청 중에는
     * DB Transaction이 활성화되어 있으면 안 된다.
     *
     * 외부 API 응답을 받은 이후
     * FootballSyncTxService에 진입하면서
     * Transaction이 시작되어야 한다.
     */
    @Test
    @DisplayName(
            "선수 API를 호출하는 동안에는 DB 트랜잭션이 활성화되어 있지 않다."
    )
    void syncPlayer_externalApiCall_withoutTransaction() {

        // given
        Long playerApiId = 10L;


        JsonNode apiResponse =
                playerResponse(
                        10L,
                        "Bukayo Saka"
                );


        given(
                footballRestClient
                        .callExternalPlayerApi(
                                playerApiId
                        )
        )
                .willAnswer(invocation -> {

                    /*
                     * 여기서 true가 나온다면
                     *
                     * 외부 API 응답을 기다리는 동안에도
                     * DB Transaction을 잡고 있다는 뜻이다.
                     *
                     * 현재 구조에서는 false가 정상이다.
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
                .syncPlayer(
                        playerApiId
                );


        // then
        then(footballRestClient)
                .should(times(1))
                .callExternalPlayerApi(
                        playerApiId
                );


        /*
         * 외부 API 호출 이후
         * TxService의 DB 작업은 정상적으로 완료되어야 한다.
         */
        assertThat(playerRepository.count())
                .isEqualTo(1);
    }


    /*
     * [기존 유지 - 중요]
     *
     * 동일한 Player Sync 요청은
     * 여러 번 들어올 수 있다.
     *
     * 향후 Scheduler를 붙였을 경우에는
     * 특히 자주 발생할 수 있다.
     *
     *
     * 상황:
     *
     * 같은 API-Football ID로
     * syncPlayer()를 두 번 실행한다.
     *
     *
     * 기대 결과:
     *
     * API 호출 = 2번
     *
     * Player 데이터 = 1건
     *
     * 즉, 중복 Player를 INSERT하지 않아야 한다.
     */
    @Test
    @DisplayName(
            "동일한 선수를 여러 번 동기화해도 Player를 중복 저장하지 않는다."
    )
    void syncPlayer_samePlayer_idempotent() {

        // given
        Long playerApiId = 10L;


        JsonNode apiResponse =
                playerResponse(
                        10L,
                        "Bukayo Saka"
                );


        given(
                footballRestClient
                        .callExternalPlayerApi(
                                playerApiId
                        )
        )
                .willReturn(
                        apiResponse
                );


        // when
        footballSyncService
                .syncPlayer(
                        playerApiId
                );


        footballSyncService
                .syncPlayer(
                        playerApiId
                );


        // then
        assertThat(playerRepository.count())
                .isEqualTo(1);


        Player player =
                playerRepository
                        .findByApiFootballId(
                                playerApiId
                        )
                        .orElseThrow();


        assertThat(player.getPlayerName())
                .isEqualTo(
                        "Bukayo Saka"
                );


        assertThat(player.getApiFootballId())
                .isEqualTo(
                        10L
                );


        /*
         * 외부 API 호출 자체는 두 번 발생한다.
         *
         * 중복 저장을 방지하는 책임은
         *
         * getOrCreatePlayer()
         *
         * 에 있다.
         */
        then(footballRestClient)
                .should(times(2))
                .callExternalPlayerApi(
                        playerApiId
                );
    }


    /*
     * [기존 유지]
     *
     * 상황:
     *
     * DB에 이미
     *
     * apiFootballId = 10
     *
     * 인 Bukayo Saka가 존재한다.
     *
     * 이후 API-Football에서
     * 같은 선수를 다시 반환한다.
     *
     *
     * 기대 결과:
     *
     * 새로운 Player Entity를 만들지 않고
     * 기존 Player를 재사용해야 한다.
     *
     * 단순 count뿐 아니라 PK까지 비교한다.
     */
    @Test
    @DisplayName(
            "이미 존재하는 선수는 새로 저장하지 않고 기존 Player를 사용한다."
    )
    void syncPlayer_existingPlayer_reuse() {

        // given
        Player existingPlayer =
                playerRepository.save(
                        Player.of(
                                "Bukayo Saka",
                                10L
                        )
                );


        JsonNode apiResponse =
                playerResponse(
                        10L,
                        "Bukayo Saka"
                );


        given(
                footballRestClient
                        .callExternalPlayerApi(
                                10L
                        )
        )
                .willReturn(
                        apiResponse
                );


        // when
        PlayerSaveResponseDto response =
                footballSyncService
                        .syncPlayer(
                                10L
                        );


        // then
        assertThat(playerRepository.count())
                .isEqualTo(1);


        Player foundPlayer =
                playerRepository
                        .findByApiFootballId(
                                10L
                        )
                        .orElseThrow();


        /*
         * 새로운 Entity를 INSERT한 것이 아니라
         * 기존 Entity가 그대로 재사용됐는지 확인한다.
         */
        assertThat(foundPlayer.getId())
                .isEqualTo(
                        existingPlayer.getId()
                );


        assertThat(foundPlayer.getPlayerName())
                .isEqualTo(
                        "Bukayo Saka"
                );


        /*
         * 기존 Entity를 재사용한 경우에도
         * TxService 안에서 DTO 변환이 정상적으로 완료되어야 한다.
         */
        assertThat(response.getPlayerName())
                .isEqualTo(
                        "Bukayo Saka"
                );


        assertThat(response.getPlayerAPIId())
                .isEqualTo(
                        10L
                );
    }


    // ==================================================
    // 예외 상황
    // ==================================================


    /*
     * [기존 유지]
     *
     * 상황:
     *
     * API-Football 호출 자체에서
     * RuntimeException이 발생한다.
     *
     *
     * 아직 FootballSyncTxService에
     * 진입하기 전이기 때문에
     * DB Transaction도 시작되지 않는다.
     *
     *
     * 기대 결과:
     *
     * Player 데이터는 아무것도 저장되지 않는다.
     */
    @Test
    @DisplayName(
            "선수 API 호출에 실패하면 Player 데이터는 변경되지 않는다."
    )
    void syncPlayer_externalApiFail_noDatabaseChange() {

        // given
        Long playerApiId = 10L;


        given(
                footballRestClient
                        .callExternalPlayerApi(
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
                        .syncPlayer(
                                playerApiId
                        )
        )
                .isInstanceOf(
                        RuntimeException.class
                )
                .hasMessage(
                        "API-Football 호출 실패"
                );


        /*
         * TxService에 진입하지 않았으므로
         * DB에는 아무 변화도 없어야 한다.
         */
        assertThat(playerRepository.count())
                .isZero();


        then(footballRestClient)
                .should(times(1))
                .callExternalPlayerApi(
                        playerApiId
                );
    }


    // ==================================================
    // Fixture
    // ==================================================


    /*
     * [Fixture - syncPlayer 선수 응답]
     *
     * 사용 대상:
     *
     * FootballSyncService.syncPlayer()
     *
     *
     * 현재 ApiFootballHttpClient는
     * API-Football의 전체 JSON을 그대로 반환하지 않는다.
     *
     * 내부에서
     *
     * response[0].player
     *
     * 를 추출한 뒤 FootballSyncService에 반환한다.
     *
     *
     * 따라서 이 Fixture는:
     *
     * {
     *   "id": 10,
     *   "name": "Bukayo Saka"
     * }
     *
     * 형태가 맞다.
     */
    private JsonNode playerResponse(
            Long playerApiId,
            String playerName
    ) {

        return readJson(
                """
                {
                  "id": %d,
                  "name": "%s"
                }
                """
                        .formatted(
                                playerApiId,
                                playerName
                        )
        );
    }


    /*
     * [Fixture Helper]
     *
     * 테스트 JSON 문자열을
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