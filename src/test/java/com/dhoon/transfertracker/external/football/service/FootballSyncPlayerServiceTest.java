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
 * 하지만 실제 Football Sync 구조에서는
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
     * [기존 유지]
     *
     * 나타내는 상황:
     *
     * DB에는 아직 Bukayo Saka가 존재하지 않는다.
     *
     * API-Football에서
     *
     * id   = 10
     * name = Bukayo Saka
     *
     * 를 반환한다.
     *
     *
     * 현재 리팩토링 이후 흐름:
     *
     * FootballSyncService.syncPlayer()
     *
     *      ↓
     *
     * 외부 API 호출
     *
     *      ↓
     *
     * FootballSyncTxService.getOrCreatePlayerResponse()
     *
     *      ↓
     *
     * Player 조회 / 생성
     *
     *      ↓
     *
     * Transaction 안에서 PlayerSaveResponseDto 생성
     *
     *
     * 기대 결과:
     *
     * Player 1건 저장
     *
     * +
     *
     * 정상적인 PlayerSaveResponseDto 반환
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
         * Player Entity가 FootballSyncService까지 반환되는 것이 아니라
         * FootballSyncTxService 내부에서 DTO로 변환된 결과가 반환된다.
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
         * 실제 DB에도 Player가 한 건 저장되어야 한다.
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
     * 이번 리팩토링에서도 반드시 유지되어야 하는 Transaction 경계.
     *
     *
     * 나타내는 상황:
     *
     * FootballSyncService가
     * API-Football에 선수 정보를 요청한다.
     *
     *
     * 기대 결과:
     *
     * 외부 HTTP 요청을 수행하고 있는 동안에는
     * DB Transaction이 활성화되어 있지 않아야 한다.
     *
     *
     * Transaction은 API 응답을 모두 받은 뒤
     *
     * FootballSyncTxService
     *
     * 에 진입하면서 시작되어야 한다.
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
                     * 여기에서 true가 나오면
                     *
                     * API-Football 응답을 기다리는 동안에도
                     * DB Transaction을 잡고 있다는 의미다.
                     *
                     * 현재 설계에서는 false가 정상.
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
     * [기존 유지 - 중요]
     *
     * 동일한 Player Sync는 여러 번 발생할 수 있다.
     *
     * 특히 향후 Scheduler를 사용하면
     * 동일 선수를 다시 조회할 가능성이 높다.
     *
     *
     * 나타내는 상황:
     *
     * 같은 API-Football player ID로
     * syncPlayer()를 두 번 실행한다.
     *
     *
     * 기대 결과:
     *
     * API 호출은 2번
     *
     * 하지만
     *
     * Player는 DB에 1건만 존재해야 한다.
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
         * API 호출 자체는 두 번 발생한다.
         *
         * 중복 저장을 막는 책임은
         * 외부 API Client가 아니라
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
     * 나타내는 상황:
     *
     * DB에 이미
     *
     * apiFootballId = 10
     *
     * 인 Bukayo Saka가 존재한다.
     *
     *
     * API-Football에서 같은 선수를 다시 반환한다.
     *
     *
     * 기대 결과:
     *
     * 새로운 Player를 생성하지 않고
     * 기존 Player를 재사용해야 한다.
     *
     *
     * 단순히 count == 1만 보는 것이 아니라
     * 기존 PK까지 비교해서
     *
     * "진짜 기존 Entity를 사용했는가?"
     *
     * 를 검증한다.
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
         * 새로운 Player가 INSERT된 것이 아니라
         * 기존 Player가 재사용되었는지 PK까지 비교한다.
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
         * 기존 Player를 사용했더라도
         * 응답 DTO는 정상적으로 생성되어야 한다.
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
     * 나타내는 상황:
     *
     * API-Football 호출 자체가 실패한다.
     *
     *
     * 아직
     *
     * FootballSyncTxService
     *
     * 에 진입하기 전이므로
     * DB Transaction 자체가 시작되지 않는다.
     *
     *
     * 기대 결과:
     *
     * Player 데이터는 아무것도 저장되지 않아야 한다.
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
     * 현재 ApiFootballHttpClient는
     * API-Football 전체 응답을 그대로 반환하는 것이 아니라
     *
     * response[0].player
     *
     * 부분을 추출해서 Service에 반환한다.
     *
     *
     * 따라서 이 Fixture는 전체 API 응답이 아니라
     *
     * FootballSyncService가 실제로 전달받는
     * Player JsonNode
     *
     * 를 표현한다.
     *
     *
     * 사용 테스트:
     *
     * 1. 새로운 Player 정상 저장
     * 2. 외부 API 호출 중 Transaction 비활성
     * 3. 동일 Player 반복 Sync / 멱등성
     * 4. 기존 Player 재사용
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
     * 문자열로 만든 테스트 Fixture를
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