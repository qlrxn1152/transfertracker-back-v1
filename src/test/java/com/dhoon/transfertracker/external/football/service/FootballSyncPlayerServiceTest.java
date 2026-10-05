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
 * [신규 - 중요]
 *
 * @DataJpaTest는 기본적으로 각각의 테스트 메서드를
 * Transaction으로 감싼다.
 *
 * 하지만 현재 Football Sync 구조에서는
 *
 * FootballSyncService
 *      → 외부 API 호출
 *      → Transaction X
 *
 * FootballSyncTxService
 *      → DB 작업
 *      → Transaction O
 *
 * 라는 실제 Transaction 경계를 검증해야 한다.
 *
 * 테스트 자체가 Transaction을 가지고 있으면
 * 외부 API 호출 시에도 Transaction이 활성화된 것처럼 보이므로
 * 테스트 Transaction을 비활성화한다.
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
     * 이유:
     *
     * 1. 테스트가 외부 API 상태에 의존하면 안 된다.
     * 2. API 요청 횟수를 소비하지 않아야 한다.
     * 3. 성공 / 실패 상황을 테스트에서 직접 만들 수 있어야 한다.
     */
    @MockitoBean
    ApiFootballHttpClient footballRestClient;


    private final ObjectMapper objectMapper =
            new ObjectMapper();


    @BeforeEach
    void clearDatabase() {

        /*
         * 테스트 자체의 Transaction을 비활성화했기 때문에
         * 테스트가 끝난 뒤 자동 Rollback에 의존하지 않는다.
         *
         * 따라서 각각의 테스트가 서로 영향을 주지 않도록
         * Player 데이터를 직접 삭제한다.
         */
        playerRepository.deleteAll();
    }


    // ==================================================
    // 정상 상황
    // ==================================================


    /*
     * [신규]
     *
     * syncPlayer()의 가장 기본적인 정상 동작.
     *
     * 나타내는 상황:
     *
     * DB에는 아직 Bukayo Saka가 존재하지 않고,
     * API-Football에서는 정상적으로 선수 정보를 반환한다.
     *
     * 기대 결과:
     *
     * 새로운 Player가 DB에 저장되고
     * API 응답 DTO에도 동일한 선수 정보가 반환되어야 한다.
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
     * [신규 - 중요]
     *
     * 이번 Football Sync 리팩토링의 핵심 검증.
     *
     * 나타내는 상황:
     *
     * FootballSyncService가 API-Football에
     * 선수 정보를 요청하는 순간.
     *
     * 기대 결과:
     *
     * 외부 HTTP 요청이 진행되는 동안에는
     * DB Transaction이 활성화되어 있지 않아야 한다.
     *
     * DB Transaction은 외부 응답을 받은 뒤
     * FootballSyncTxService에 진입하면서 시작되어야 한다.
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
                     * 이 값이 true라면
                     *
                     * API-Football 응답을 기다리는 동안에도
                     * DB Transaction이 열려 있다는 뜻이다.
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


        assertThat(playerRepository.count())
                .isEqualTo(1);
    }


    /*
     * [신규 - 중요]
     *
     * 동일한 선수에 대한 Sync 요청은
     * 여러 번 발생할 수 있다.
     *
     * 나타내는 상황:
     *
     * 동일한 API-Football 선수 ID를 가지고
     * syncPlayer()를 두 번 실행한다.
     *
     * 기대 결과:
     *
     * 외부 API는 두 번 호출되지만
     * DB의 Player는 한 명만 존재해야 한다.
     *
     * 즉 syncPlayer()가 멱등성을 가지는지 검증한다.
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
         * Sync 요청 자체는 두 번 발생했으므로
         * 외부 API도 두 번 호출되는 것이 정상이다.
         *
         * 중복을 방지하는 책임은
         * HTTP Client가 아니라 DB 저장 로직에 있다.
         */
        then(footballRestClient)
                .should(times(2))
                .callExternalPlayerApi(
                        playerApiId
                );
    }


    /*
     * [신규]
     *
     * DB에 이미 같은 API-Football ID를 가진
     * Player가 존재하는 상황.
     *
     * 기대 결과:
     *
     * 새로운 Player Entity를 생성하지 않고
     * 기존 Player를 그대로 재사용해야 한다.
     *
     * 단순 count뿐 아니라 PK까지 비교하여
     * 기존 Entity를 실제로 재사용했는지 검증한다.
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
         * 새로운 Player가 만들어진 것이 아니라
         * 기존 Player가 재사용되었는지 PK로 확인한다.
         */
        assertThat(foundPlayer.getId())
                .isEqualTo(
                        existingPlayer.getId()
                );


        assertThat(foundPlayer.getPlayerName())
                .isEqualTo(
                        "Bukayo Saka"
                );


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
     * [신규]
     *
     * API-Football 호출 자체가 실패한 상황.
     *
     * 나타내는 상황:
     *
     * FootballSyncService에서 외부 API를 호출했지만
     * RuntimeException이 발생하여 응답을 받지 못했다.
     *
     * 기대 결과:
     *
     * 아직 FootballSyncTxService에 진입하기 전이므로
     * Player 데이터는 DB에 아무것도 저장되지 않아야 한다.
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
     * 나타내는 상황:
     *
     * API-Football의
     *
     * GET /players/profiles?player={playerApiId}
     *
     * 요청이 성공하여 특정 선수 정보를 반환한 상황을 표현한다.
     *
     *
     * 실제 API-Football 원본 응답은 대략
     *
     * response
     *   └ player
     *
     * 구조를 가지고 있다.
     *
     * 하지만 현재
     *
     * ApiFootballHttpClient.callExternalPlayerApi()
     *
     * 내부에서 이미
     *
     * response[0].player
     *
     * 부분만 추출해서 FootballSyncService로 반환한다.
     *
     *
     * 따라서 이 Fixture는
     * API-Football 전체 JSON 응답이 아니라,
     *
     * FootballSyncService가 실제로 전달받는
     * Player JsonNode 자체를 표현한다.
     *
     *
     * 사용되는 테스트 상황:
     *
     * 1. 새로운 Player 정상 저장
     * 2. HTTP 호출 시 Transaction 비활성 검증
     * 3. 동일 Player 반복 Sync / 멱등성 검증
     * 4. 기존 Player 재사용 검증
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
     * [Fixture Helper - JSON 변환]
     *
     * 문자열로 작성한 테스트 JSON을
     * JsonNode로 변환하기 위한 공통 Helper.
     *
     * Fixture JSON 자체에 문제가 있다면
     * 테스트 실패 원인을 명확하게 확인할 수 있도록
     * IllegalStateException으로 변환한다.
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