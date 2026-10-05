package com.dhoon.transfertracker.external.football.service;

import com.dhoon.transfertracker.external.football.client.ApiFootballHttpClient;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.teamplayer.repository.TeamPlayerRepository;
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
 * 하지만 실제 syncLeagueTeams() 구조는
 *
 * FootballSyncService
 *      → API-Football 호출
 *      → Transaction X
 *
 * FootballSyncTxService
 *      → Team 조회 / 생성
 *      → leagueCode 할당
 *      → Transaction O
 *
 * 이다.
 *
 * 또한 syncLeagueTeams()는 여러 Team을 순회하면서
 * getOrCreateTeamAndAssignLeague()를 각각 호출한다.
 *
 * 따라서 Team 하나마다 별도의 Transaction이 실행되는
 * 실제 애플리케이션 동작을 검증하기 위해
 * Test 자체 Transaction은 비활성화한다.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FootballSyncLeagueTeamsServiceTest {


    @Autowired
    FootballSyncService footballSyncService;


    @Autowired
    TeamRepository teamRepository;


    /*
     * 아래 Repository들은 syncLeagueTeams() 자체에서 사용하는 것은 아니다.
     *
     * 하지만 Test 자체 Transaction을 비활성화했기 때문에
     * 다른 테스트에서 Commit된 데이터가 남아 있을 수 있다.
     *
     * Transfer / TeamPlayer가 Team을 FK로 참조하므로
     * 안전한 테스트 격리를 위해 같이 정리한다.
     */
    @Autowired
    PlayerRepository playerRepository;

    @Autowired
    TeamPlayerRepository teamPlayerRepository;

    @Autowired
    TransferRepository transferRepository;


    /*
     * 실제 API-Football 서버는 호출하지 않는다.
     *
     * 테스트에서
     *
     * - 정상 리그 팀 응답
     * - 빈 응답
     * - API 실패
     * - 중간 Team 데이터 오류
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
         * FK 자식 Entity부터 삭제한다.
         *
         * Transfer
         * TeamPlayer
         * Player
         * Team
         *
         * 순서로 정리하여 테스트 간 데이터 영향을 제거한다.
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
     * API-Football에서 EPL 소속 Team으로
     *
     * Arsenal
     * Chelsea
     *
     * 두 팀을 정상적으로 반환한다.
     *
     *
     * 기대 결과:
     *
     * Team 2건이 저장되고
     * 두 Team 모두 leagueCode = EPL 이어야 한다.
     *
     * 특히 assignTeamLeague()가 Transaction 안에서 실행되어
     * Dirty Checking을 통해 DB에 실제 반영되는지 확인한다.
     */
    @Test
    @DisplayName(
            "리그 팀을 동기화하면 Team이 저장되고 leagueCode가 DB에 반영된다."
    )
    void syncLeagueTeams_success() {

        // given
        LeagueCode leagueCode =
                LeagueCode.EPL;


        JsonNode apiResponse =
                leagueTeamsResponse();


        given(
                footballRestClient
                        .callExternalLeagueTeamsApi(
                                leagueCode
                        )
        )
                .willReturn(
                        apiResponse
                );


        // when
        String response =
                footballSyncService
                        .syncLeagueTeams(
                                leagueCode
                        );


        // then
        assertThat(response)
                .isEqualTo(
                        "FootballSyncService.syncLeagueTeams"
                );


        assertThat(teamRepository.count())
                .isEqualTo(2);


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


        assertThat(arsenal.getTeamName())
                .isEqualTo(
                        "Arsenal"
                );


        assertThat(chelsea.getTeamName())
                .isEqualTo(
                        "Chelsea"
                );


        assertThat(arsenal.getLeagueCode())
                .isEqualTo(
                        LeagueCode.EPL
                );


        assertThat(chelsea.getLeagueCode())
                .isEqualTo(
                        LeagueCode.EPL
                );
    }


    /*
     * [신규 - 중요]
     *
     * 나타내는 상황:
     *
     * FootballSyncService가
     * 리그의 Team 정보를 API-Football에 요청하는 순간.
     *
     *
     * 기대 결과:
     *
     * 외부 HTTP 응답을 기다리는 동안에는
     * DB Transaction이 활성화되어 있지 않아야 한다.
     *
     * Transaction은 API 응답을 모두 받은 이후
     * 각 Team을 DB에 반영할 때 시작되어야 한다.
     */
    @Test
    @DisplayName(
            "리그 팀 API를 호출하는 동안에는 DB 트랜잭션이 활성화되어 있지 않다."
    )
    void syncLeagueTeams_externalApiCall_withoutTransaction() {

        // given
        LeagueCode leagueCode =
                LeagueCode.EPL;


        JsonNode apiResponse =
                leagueTeamsResponse();


        given(
                footballRestClient
                        .callExternalLeagueTeamsApi(
                                leagueCode
                        )
        )
                .willAnswer(invocation -> {

                    /*
                     * true라면
                     *
                     * API-Football 응답을 기다리는 동안에도
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
                .syncLeagueTeams(
                        leagueCode
                );


        // then
        then(footballRestClient)
                .should(times(1))
                .callExternalLeagueTeamsApi(
                        leagueCode
                );


        assertThat(teamRepository.count())
                .isEqualTo(2);
    }


    /*
     * [신규 - 중요]
     *
     * 나타내는 상황:
     *
     * 동일한 EPL Team 데이터를 가지고
     * syncLeagueTeams()를 두 번 실행한다.
     *
     *
     * 기대 결과:
     *
     * 두 번째 Sync에서도
     * apiFootballId 기준으로 기존 Team을 재사용해야 한다.
     *
     * 따라서 Team 개수는 2개로 유지되어야 한다.
     */
    @Test
    @DisplayName(
            "동일한 리그 팀 목록을 여러 번 동기화해도 Team을 중복 저장하지 않는다."
    )
    void syncLeagueTeams_sameData_idempotent() {

        // given
        LeagueCode leagueCode =
                LeagueCode.EPL;


        JsonNode apiResponse =
                leagueTeamsResponse();


        given(
                footballRestClient
                        .callExternalLeagueTeamsApi(
                                leagueCode
                        )
        )
                .willReturn(
                        apiResponse
                );


        // when
        footballSyncService
                .syncLeagueTeams(
                        leagueCode
                );


        Team firstArsenal =
                teamRepository
                        .findByApiFootballId(
                                42L
                        )
                        .orElseThrow();


        Long firstArsenalId =
                firstArsenal.getId();


        footballSyncService
                .syncLeagueTeams(
                        leagueCode
                );


        // then
        assertThat(teamRepository.count())
                .isEqualTo(2);


        Team secondArsenal =
                teamRepository
                        .findByApiFootballId(
                                42L
                        )
                        .orElseThrow();


        /*
         * 단순히 count == 2만 확인하지 않고
         * 기존 Arsenal Entity가 실제로 재사용됐는지
         * PK까지 확인한다.
         */
        assertThat(secondArsenal.getId())
                .isEqualTo(
                        firstArsenalId
                );


        assertThat(secondArsenal.getLeagueCode())
                .isEqualTo(
                        LeagueCode.EPL
                );


        then(footballRestClient)
                .should(times(2))
                .callExternalLeagueTeamsApi(
                        leagueCode
                );
    }


    /*
     * [신규]
     *
     * 나타내는 상황:
     *
     * Arsenal Team은 이미 DB에 존재하지만
     * 아직 leagueCode가 지정되지 않은 상태다.
     *
     * 이후 EPL Team Sync를 실행한다.
     *
     *
     * 기대 결과:
     *
     * 새로운 Arsenal을 생성하지 않고
     * 기존 Team을 재사용해야 한다.
     *
     * 그리고 기존 Team에
     * leagueCode = EPL이 Dirty Checking으로 반영되어야 한다.
     */
    @Test
    @DisplayName(
            "이미 존재하는 Team은 재사용하고 leagueCode만 반영한다."
    )
    void syncLeagueTeams_existingTeam_reuseAndAssignLeague() {

        // given
        Team existingArsenal =
                teamRepository.save(
                        Team.of(
                                "Arsenal",
                                42L
                        )
                );


        Long existingTeamId =
                existingArsenal.getId();


        assertThat(existingArsenal.getLeagueCode())
                .isNull();


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
        /*
         * 기존 Arsenal 1건
         * +
         * API 응답에서 새로 생성된 Chelsea 1건
         */
        assertThat(teamRepository.count())
                .isEqualTo(2);


        Team foundArsenal =
                teamRepository
                        .findByApiFootballId(
                                42L
                        )
                        .orElseThrow();


        /*
         * 새로운 Arsenal이 아니라
         * 기존 Team Entity가 재사용되었는지 확인한다.
         */
        assertThat(foundArsenal.getId())
                .isEqualTo(
                        existingTeamId
                );


        /*
         * Transaction 안에서
         * assignTeamLeague(EPL)이 실행되고
         *
         * Commit 시 Dirty Checking으로
         * UPDATE가 발생했는지 확인한다.
         */
        assertThat(foundArsenal.getLeagueCode())
                .isEqualTo(
                        LeagueCode.EPL
                );
    }


    /*
     * [신규 - 경계]
     *
     * 나타내는 상황:
     *
     * API-Football 호출 자체는 성공했지만
     * response 배열이 비어있는 상황.
     *
     *
     * 기대 결과:
     *
     * 처리할 Team이 없으므로
     * DB에는 Team이 생성되지 않아야 한다.
     */
    @Test
    @DisplayName(
            "리그 팀 응답이 비어있으면 Team을 생성하지 않는다."
    )
    void syncLeagueTeams_emptyResponse_noDatabaseChange() {

        // given
        LeagueCode leagueCode =
                LeagueCode.EPL;


        JsonNode apiResponse =
                leagueTeamsEmptyResponse();


        given(
                footballRestClient
                        .callExternalLeagueTeamsApi(
                                leagueCode
                        )
        )
                .willReturn(
                        apiResponse
                );


        // when
        String response =
                footballSyncService
                        .syncLeagueTeams(
                                leagueCode
                        );


        // then
        assertThat(response)
                .isEqualTo(
                        "FootballSyncService.syncLeagueTeams"
                );


        assertThat(teamRepository.count())
                .isZero();
    }


    // ==================================================
    // 예외 상황
    // ==================================================


    /*
     * [신규]
     *
     * 나타내는 상황:
     *
     * API-Football의 리그 Team API 호출 자체가 실패한다.
     *
     *
     * 기대 결과:
     *
     * 아직 FootballSyncTxService에 진입하기 전이므로
     * DB에는 Team 데이터가 생성되지 않아야 한다.
     */
    @Test
    @DisplayName(
            "리그 팀 API 호출에 실패하면 Team 데이터는 변경되지 않는다."
    )
    void syncLeagueTeams_externalApiFail_noDatabaseChange() {

        // given
        LeagueCode leagueCode =
                LeagueCode.EPL;


        given(
                footballRestClient
                        .callExternalLeagueTeamsApi(
                                leagueCode
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
                        .syncLeagueTeams(
                                leagueCode
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
                .callExternalLeagueTeamsApi(
                        leagueCode
                );
    }


    /*
     * [신규 - 중요]
     *
     * 나타내는 상황:
     *
     * API-Football에서 세 Team을 순서대로 반환한다.
     *
     * 1. Arsenal
     *      → 정상 데이터
     *      → Transaction 성공
     *      → COMMIT
     *
     * 2. Chelsea
     *      → name 필드가 없는 잘못된 데이터
     *      → Transaction 실패
     *      → ROLLBACK
     *
     * 3. Liverpool
     *      → 정상 데이터
     *      → 하지만 Chelsea 처리 중 발생한 예외가
     *        밖으로 전파되므로 현재 구현에서는 처리되지 않는다.
     *
     *
     * 기대 결과:
     *
     * Arsenal은 DB에 남는다.
     *
     * Chelsea는 Rollback된다.
     *
     * Liverpool은 처리되지 않는다.
     *
     *
     * saveTeamTransfers()와 동일하게
     * 현재 syncLeagueTeams()가 Team 단위의
     * Partial Commit 구조라는 것을 확인한다.
     */
    @Test
    @DisplayName(
            "중간 Team 처리에 실패하면 이전 Team은 유지되고 이후 Team은 처리되지 않는다."
    )
    void syncLeagueTeams_secondTeamFail_partialCommitAndStop() {

        // given
        LeagueCode leagueCode =
                LeagueCode.EPL;


        JsonNode apiResponse =
                leagueTeamsSecondTeamInvalidResponse();


        given(
                footballRestClient
                        .callExternalLeagueTeamsApi(
                                leagueCode
                        )
        )
                .willReturn(
                        apiResponse
                );


        // when & then
        assertThatThrownBy(() ->
                footballSyncService
                        .syncLeagueTeams(
                                leagueCode
                        )
        )
                .isInstanceOf(
                        NullPointerException.class
                );


        /*
         * 첫 번째 Arsenal Transaction은
         * 이미 성공하여 COMMIT되었다.
         */
        Team arsenal =
                teamRepository
                        .findByApiFootballId(
                                42L
                        )
                        .orElseThrow();


        assertThat(arsenal.getTeamName())
                .isEqualTo(
                        "Arsenal"
                );


        assertThat(arsenal.getLeagueCode())
                .isEqualTo(
                        LeagueCode.EPL
                );


        /*
         * 두 번째 Chelsea는
         * name 필드가 없는 상태에서 신규 Team 생성 중
         * 예외가 발생했으므로 저장되지 않아야 한다.
         */
        assertThat(
                teamRepository
                        .findByApiFootballId(
                                49L
                        )
        )
                .isEmpty();


        /*
         * 세 번째 Liverpool은
         * Chelsea의 예외가 syncLeagueTeams() 밖으로
         * 전파되면서 forEach가 중단됐기 때문에
         * 처리 자체가 시작되지 않는다.
         */
        assertThat(
                teamRepository
                        .findByApiFootballId(
                                40L
                        )
        )
                .isEmpty();


        /*
         * 최종 DB에는 첫 번째 Arsenal만 남는다.
         */
        assertThat(teamRepository.count())
                .isEqualTo(1);
    }


    // ==================================================
    // Fixture
    // ==================================================


    /*
     * [Fixture - 정상 EPL Team 목록]
     *
     * 사용 대상:
     *
     * FootballSyncService.syncLeagueTeams()
     *
     *
     * 나타내는 상황:
     *
     * API-Football의
     *
     * GET /teams?league={leagueId}&season=2024
     *
     * 요청에서
     *
     * Arsenal
     * Chelsea
     *
     * 두 Team을 정상적으로 반환한 상황.
     *
     *
     * 실제 syncLeagueTeams()에서는
     *
     * response
     *   └ team
     *
     * 데이터를 꺼내
     * FootballSyncTxService에 전달한다.
     *
     *
     * 사용되는 테스트:
     *
     * 1. Team 정상 저장 + leagueCode 할당
     * 2. 외부 API 호출 시 Transaction 비활성
     * 3. 동일 데이터 반복 Sync / 멱등성
     * 4. 기존 Team 재사용 + leagueCode 반영
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


    /*
     * [Fixture - Team이 없는 리그 응답]
     *
     * 사용 대상:
     *
     * syncLeagueTeams_emptyResponse_noDatabaseChange()
     *
     *
     * 나타내는 상황:
     *
     * API-Football 요청 자체는 성공했지만
     * response 배열이 비어있는 상황.
     *
     * 따라서 syncLeagueTeams()의 forEach는
     * 한 번도 실행되지 않는다.
     */
    private JsonNode leagueTeamsEmptyResponse() {

        return readJson(
                """
                {
                  "response": []
                }
                """
        );
    }


    /*
     * [Fixture - 두 번째 Team 데이터가 잘못된 리그 응답]
     *
     * 사용 대상:
     *
     * syncLeagueTeams_secondTeamFail_partialCommitAndStop()
     *
     *
     * 나타내는 상황:
     *
     * 첫 번째 Arsenal은 정상 데이터라
     * Team 저장과 leagueCode 할당이 Commit된다.
     *
     * 두 번째 Chelsea는 id만 존재하고 name이 없다.
     *
     * 현재 getOrCreateTeam()은 신규 Team 생성 시
     *
     * teamData.get("name").asString()
     *
     * 을 호출하므로 여기서 예외가 발생한다.
     *
     * 세 번째 Liverpool은 정상 데이터지만
     * 두 번째 Team의 예외로 인해 처리되지 않는다.
     *
     *
     * 이 Fixture를 통해
     *
     * - Team 단위 Transaction
     * - 이전 Team Partial Commit
     * - 예외 이후 forEach 중단
     *
     * 을 검증한다.
     */
    private JsonNode leagueTeamsSecondTeamInvalidResponse() {

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
                        "id": 49
                      }
                    },
                    {
                      "team": {
                        "id": 40,
                        "name": "Liverpool"
                      }
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