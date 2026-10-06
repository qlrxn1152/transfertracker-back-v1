package com.dhoon.transfertracker.external.football.service;

import com.dhoon.transfertracker.external.football.client.ApiFootballHttpClient;
import com.dhoon.transfertracker.external.football.dto.TeamSaveResponseDto;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
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
 * @DataJpaTest는 기본적으로 각각의 테스트를
 * Transaction으로 감싼다.
 *
 * 하지만 실제 Football Sync 구조에서는
 *
 * FootballSyncService
 *      → 외부 API 호출
 *      → Transaction X
 *
 * FootballSyncTxService
 *      → Team 조회 / 저장
 *      → DTO 생성
 *      → Transaction O
 *
 * 라는 실제 Transaction 경계를 검증해야 한다.
 *
 * 따라서 Test 자체 Transaction은 비활성화한다.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FootballSyncTeamServiceTest {


    @Autowired
    FootballSyncService footballSyncService;


    @Autowired
    TeamRepository teamRepository;


    /*
     * 실제 API-Football 서버는 호출하지 않는다.
     *
     * 테스트에서
     *
     * - 정상 응답
     * - 외부 API 실패
     *
     * 상황을 직접 만들기 위해 Mock 처리한다.
     */
    @MockitoBean
    ApiFootballHttpClient footballRestClient;


    private final ObjectMapper objectMapper =
            new ObjectMapper();


    @BeforeEach
    void clearDatabase() {

        /*
         * Test Transaction을 비활성화했으므로
         * 각 테스트가 끝난 뒤 자동 Rollback되지 않는다.
         *
         * 테스트 간 데이터 간섭을 막기 위해
         * Team 데이터를 직접 삭제한다.
         */
        teamRepository.deleteAll();
    }


    // ==================================================
    // 정상 상황
    // ==================================================


    /*
     * [기존 유지]
     *
     * syncTeam()의 가장 기본적인 정상 동작.
     *
     *
     * 상황:
     *
     * DB에는 Arsenal이 존재하지 않는다.
     *
     * API-Football에서는
     *
     * id   = 42
     * name = Arsenal
     *
     * 을 반환한다.
     *
     *
     * 현재 리팩토링 이후 흐름:
     *
     * FootballSyncService.syncTeam()
     *
     *      ↓
     *
     * 외부 Team API 호출
     *
     *      ↓
     *
     * FootballSyncTxService.getOrCreateTeamResponse()
     *
     *      ↓
     *
     * getOrCreateTeam()
     *
     *      ↓
     *
     * Team 조회 / 저장
     *
     *      ↓
     *
     * Transaction 내부에서
     * TeamSaveResponseDto 생성
     *
     *
     * 기대 결과:
     *
     * Team 1건 저장
     *
     * +
     *
     * 정상적인 TeamSaveResponseDto 반환
     */
    @Test
    @DisplayName(
            "팀 정보를 동기화하면 새로운 Team이 저장된다."
    )
    void syncTeam_success() {

        // given
        Long teamApiId = 42L;


        JsonNode apiResponse =
                teamResponse(
                        42L,
                        "Arsenal"
                );


        given(
                footballRestClient
                        .callExternalTeamApi(
                                teamApiId
                        )
        )
                .willReturn(
                        apiResponse
                );


        // when
        TeamSaveResponseDto response =
                footballSyncService
                        .syncTeam(
                                teamApiId
                        );


        // then

        /*
         * [리팩토링 확인]
         *
         * Team Entity가 FootballSyncService로 반환되는 것이 아니라
         * FootballSyncTxService 내부에서
         * TeamSaveResponseDto로 변환된 결과가 반환된다.
         */
        assertThat(response.getTeamName())
                .isEqualTo(
                        "Arsenal"
                );


        assertThat(response.getTeamAPIId())
                .isEqualTo(
                        42L
                );


        /*
         * 실제 DB에도 Team이 저장되어야 한다.
         */
        assertThat(teamRepository.count())
                .isEqualTo(1);


        Team team =
                teamRepository
                        .findByApiFootballId(
                                teamApiId
                        )
                        .orElseThrow();


        assertThat(team.getTeamName())
                .isEqualTo(
                        "Arsenal"
                );


        assertThat(team.getApiFootballId())
                .isEqualTo(
                        42L
                );
    }


    /*
     * [기존 유지 - 중요]
     *
     * 이번 리팩토링에서도 유지되어야 하는
     * Transaction 경계를 검증한다.
     *
     *
     * 상황:
     *
     * FootballSyncService가
     * API-Football Team API를 호출한다.
     *
     *
     * 기대 결과:
     *
     * 외부 HTTP 요청 동안에는
     * DB Transaction이 활성화되어 있지 않아야 한다.
     *
     * Transaction은 API 응답을 받은 뒤
     * FootballSyncTxService에 진입하면서 시작된다.
     */
    @Test
    @DisplayName(
            "팀 API를 호출하는 동안에는 DB 트랜잭션이 활성화되어 있지 않다."
    )
    void syncTeam_externalApiCall_withoutTransaction() {

        // given
        Long teamApiId = 42L;


        JsonNode apiResponse =
                teamResponse(
                        42L,
                        "Arsenal"
                );


        given(
                footballRestClient
                        .callExternalTeamApi(
                                teamApiId
                        )
        )
                .willAnswer(invocation -> {

                    /*
                     * true가 나오면
                     *
                     * API-Football 응답을 기다리는 동안에도
                     * DB Transaction을 유지하고 있다는 의미다.
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
                .syncTeam(
                        teamApiId
                );


        // then
        then(footballRestClient)
                .should(times(1))
                .callExternalTeamApi(
                        teamApiId
                );


        /*
         * API 호출이 끝난 뒤
         * TxService를 통한 저장은 정상적으로 수행되어야 한다.
         */
        assertThat(teamRepository.count())
                .isEqualTo(1);
    }


    /*
     * [기존 유지 - 중요]
     *
     * 동일한 Team Sync는 여러 번 발생할 수 있다.
     *
     * 특히 향후 Scheduler를 사용하면
     * 같은 Team을 다시 동기화할 가능성이 높다.
     *
     *
     * 상황:
     *
     * 같은 API-Football team ID로
     * syncTeam()을 두 번 실행한다.
     *
     *
     * 기대 결과:
     *
     * 외부 API 호출 = 2번
     *
     * DB Team = 1건
     *
     * 즉 같은 Team을 중복 INSERT하면 안 된다.
     */
    @Test
    @DisplayName(
            "동일한 팀을 여러 번 동기화해도 Team을 중복 저장하지 않는다."
    )
    void syncTeam_sameTeam_idempotent() {

        // given
        Long teamApiId = 42L;


        JsonNode apiResponse =
                teamResponse(
                        42L,
                        "Arsenal"
                );


        given(
                footballRestClient
                        .callExternalTeamApi(
                                teamApiId
                        )
        )
                .willReturn(
                        apiResponse
                );


        // when
        footballSyncService
                .syncTeam(
                        teamApiId
                );


        footballSyncService
                .syncTeam(
                        teamApiId
                );


        // then
        assertThat(teamRepository.count())
                .isEqualTo(1);


        Team team =
                teamRepository
                        .findByApiFootballId(
                                teamApiId
                        )
                        .orElseThrow();


        assertThat(team.getTeamName())
                .isEqualTo(
                        "Arsenal"
                );


        assertThat(team.getApiFootballId())
                .isEqualTo(
                        42L
                );


        /*
         * Sync 요청 자체는 두 번이므로
         * 외부 API도 두 번 호출되는 것이 정상이다.
         *
         * 중복 저장 방지는
         *
         * getOrCreateTeam()
         *
         * 이 담당한다.
         */
        then(footballRestClient)
                .should(times(2))
                .callExternalTeamApi(
                        teamApiId
                );
    }


    /*
     * [기존 유지]
     *
     * 상황:
     *
     * DB에 이미
     *
     * apiFootballId = 42
     *
     * 인 Arsenal이 존재한다.
     *
     * API-Football에서도
     * 동일한 Arsenal을 반환한다.
     *
     *
     * 기대 결과:
     *
     * 새로운 Team을 생성하지 않고
     * 기존 Team을 재사용해야 한다.
     *
     * 단순히 count == 1만 확인하는 것이 아니라
     * PK까지 비교한다.
     */
    @Test
    @DisplayName(
            "이미 존재하는 팀은 새로 저장하지 않고 기존 Team을 사용한다."
    )
    void syncTeam_existingTeam_reuse() {

        // given
        Team existingTeam =
                teamRepository.save(
                        Team.of(
                                "Arsenal",
                                42L
                        )
                );


        JsonNode apiResponse =
                teamResponse(
                        42L,
                        "Arsenal"
                );


        given(
                footballRestClient
                        .callExternalTeamApi(
                                42L
                        )
        )
                .willReturn(
                        apiResponse
                );


        // when
        TeamSaveResponseDto response =
                footballSyncService
                        .syncTeam(
                                42L
                        );


        // then
        assertThat(teamRepository.count())
                .isEqualTo(1);


        Team foundTeam =
                teamRepository
                        .findByApiFootballId(
                                42L
                        )
                        .orElseThrow();


        /*
         * 새로운 Team Entity가 만들어진 것이 아니라
         * 기존 Team이 실제로 재사용됐는지 확인한다.
         */
        assertThat(foundTeam.getId())
                .isEqualTo(
                        existingTeam.getId()
                );


        assertThat(foundTeam.getTeamName())
                .isEqualTo(
                        "Arsenal"
                );


        /*
         * 기존 Team을 재사용한 상황에서도
         * TxService 내부 DTO 변환은 정상적으로 수행되어야 한다.
         */
        assertThat(response.getTeamName())
                .isEqualTo(
                        "Arsenal"
                );


        assertThat(response.getTeamAPIId())
                .isEqualTo(
                        42L
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
     * API-Football Team API 호출 자체에서
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
     * Team 데이터는 아무것도 저장되지 않아야 한다.
     */
    @Test
    @DisplayName(
            "팀 API 호출에 실패하면 Team 데이터는 변경되지 않는다."
    )
    void syncTeam_externalApiFail_noDatabaseChange() {

        // given
        Long teamApiId = 42L;


        given(
                footballRestClient
                        .callExternalTeamApi(
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
                        .syncTeam(
                                teamApiId
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
         * DB 상태는 그대로여야 한다.
         */
        assertThat(teamRepository.count())
                .isZero();


        then(footballRestClient)
                .should(times(1))
                .callExternalTeamApi(
                        teamApiId
                );
    }


    // ==================================================
    // Fixture
    // ==================================================


    /*
     * [Fixture - syncTeam 팀 응답]
     *
     * 사용 대상:
     *
     * FootballSyncService.syncTeam()
     *
     *
     * 현재 ApiFootballHttpClient는
     * API-Football 전체 응답을 그대로 반환하지 않는다.
     *
     * 내부에서
     *
     * response[0].team
     *
     * 부분을 추출해서 FootballSyncService에 반환한다.
     *
     *
     * 따라서 이 Fixture는:
     *
     * {
     *   "id": 42,
     *   "name": "Arsenal"
     * }
     *
     * 형태가 맞다.
     *
     *
     * 사용되는 테스트:
     *
     * 1. 새로운 Team 정상 저장
     * 2. 외부 API 호출 중 Transaction 비활성
     * 3. 동일 Team 반복 Sync / 멱등성
     * 4. 기존 Team 재사용
     */
    private JsonNode teamResponse(
            Long teamApiId,
            String teamName
    ) {

        return readJson(
                """
                {
                  "id": %d,
                  "name": "%s"
                }
                """
                        .formatted(
                                teamApiId,
                                teamName
                        )
        );
    }


    /*
     * [Fixture Helper]
     *
     * 문자열 형태의 Fixture를
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