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
 * [신규 - 중요]
 *
 * @DataJpaTest는 기본적으로 Test를 Transaction으로 감싼다.
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
 * 구조이기 때문에,
 *
 * 외부 API 호출 시 실제 Transaction이 열려 있지 않은지
 * 확인하기 위해 테스트 자체의 Transaction을 비활성화한다.
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
     * 테스트에서는 원하는 Team 응답과
     * 실패 상황을 직접 만들어서 검증한다.
     */
    @MockitoBean
    ApiFootballHttpClient footballRestClient;


    private final ObjectMapper objectMapper =
            new ObjectMapper();


    @BeforeEach
    void clearDatabase() {

        /*
         * Test 자체 Transaction을 사용하지 않으므로
         * 각각의 테스트 사이에 DB 데이터가 남지 않도록
         * 직접 삭제한다.
         */
        teamRepository.deleteAll();
    }


    // ==================================================
    // 정상 상황
    // ==================================================


    /*
     * [신규]
     *
     * syncTeam()의 가장 기본적인 정상 동작.
     *
     * 나타내는 상황:
     *
     * DB에 Arsenal이 아직 존재하지 않고,
     * API-Football에서는 정상적으로 Arsenal 정보를 반환한다.
     *
     * 기대 결과:
     *
     * 새로운 Team이 DB에 저장되고,
     * TeamSaveResponseDto에도 동일한 정보가 반환되어야 한다.
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
        assertThat(response.getTeamName())
                .isEqualTo(
                        "Arsenal"
                );


        assertThat(response.getTeamAPIId())
                .isEqualTo(
                        42L
                );


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
     * [신규 - 중요]
     *
     * FootballSyncService가 API-Football을 호출하는 동안
     * DB Transaction이 활성화되어 있지 않은지 검증한다.
     *
     * 나타내는 상황:
     *
     * syncTeam()이 외부 Team API를 호출하는 순간.
     *
     * 기대 결과:
     *
     * HTTP 요청 중에는 Transaction이 없어야 한다.
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
                     * 외부 API 응답을 기다리는 동안에도
                     * DB Transaction이 열려 있다는 의미다.
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


        assertThat(teamRepository.count())
                .isEqualTo(1);
    }


    /*
     * [신규 - 중요]
     *
     * 같은 Team Sync 요청은 여러 번 발생할 수 있다.
     *
     * 나타내는 상황:
     *
     * 동일한 API-Football Team ID로
     * syncTeam()을 두 번 실행한다.
     *
     * 기대 결과:
     *
     * API 호출은 두 번 발생하지만
     * DB의 Team은 한 건만 존재해야 한다.
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
         * Sync 자체는 두 번 실행했으므로
         * 외부 API 호출도 두 번 발생한다.
         */
        then(footballRestClient)
                .should(times(2))
                .callExternalTeamApi(
                        teamApiId
                );
    }


    /*
     * [신규]
     *
     * DB에 이미 동일한 apiFootballId를 가진
     * Team이 존재하는 상황.
     *
     * 기대 결과:
     *
     * 새로운 Team을 생성하지 않고
     * 기존 Team Entity를 그대로 재사용해야 한다.
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
         * 단순히 count == 1만 보는 것이 아니라
         * 실제 기존 Entity가 재사용되었는지 PK까지 비교한다.
         */
        assertThat(foundTeam.getId())
                .isEqualTo(
                        existingTeam.getId()
                );


        assertThat(foundTeam.getTeamName())
                .isEqualTo(
                        "Arsenal"
                );


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
     * [신규]
     *
     * API-Football Team API 호출 자체가 실패한 상황.
     *
     * 나타내는 상황:
     *
     * 외부 API 호출 단계에서 RuntimeException이 발생한다.
     *
     * 기대 결과:
     *
     * FootballSyncTxService에 진입하기 전이므로
     * DB에는 Team이 저장되지 않아야 한다.
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
     * 나타내는 상황:
     *
     * API-Football의
     *
     * GET /teams?id={teamApiId}
     *
     * 요청이 성공하여 특정 팀 정보를 반환한 상황을 표현한다.
     *
     *
     * 실제 API-Football 원본 응답은 대략
     *
     * response
     *   └ team
     *
     * 구조다.
     *
     * 하지만 현재
     *
     * ApiFootballHttpClient.callExternalTeamApi()
     *
     * 내부에서 이미
     *
     * response[0].team
     *
     * 부분만 추출해서 FootballSyncService로 반환한다.
     *
     *
     * 따라서 이 Fixture는 API-Football 전체 JSON이 아니라,
     * FootballSyncService가 실제로 전달받는
     * Team JsonNode 자체를 나타낸다.
     *
     *
     * 사용되는 테스트 상황:
     *
     * 1. 새로운 Team 정상 저장
     * 2. HTTP 호출 시 Transaction 비활성 검증
     * 3. 동일 Team 반복 Sync / 멱등성 검증
     * 4. 기존 Team 재사용 검증
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
     * [Fixture Helper - JSON 변환]
     *
     * 문자열 형태의 테스트 JSON을
     * JsonNode로 변환하는 공통 Helper.
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