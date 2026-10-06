package com.dhoon.transfertracker.external.football.service;

import com.dhoon.transfertracker.external.football.client.ApiFootballHttpClient;
import com.dhoon.transfertracker.external.football.dto.LeagueTeamsResponseDto;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.dto.response.TeamItemResponseDto;
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
 * [기존 유지 - 중요]
 *
 * @DataJpaTest는 기본적으로 각각의 테스트를
 * Transaction으로 감싼다.
 *
 * 하지만 실제 syncLeagueTeams() 구조는:
 *
 * FootballSyncService
 *      → API-Football 호출
 *      → Transaction X
 *
 * FootballSyncTxService
 *      → Team 조회 / 생성
 *      → leagueCode 할당
 *      → TeamItemResponseDto 생성
 *      → Transaction O
 *
 * 이다.
 *
 * syncLeagueTeams()는 여러 Team을 반복하면서
 *
 * getOrCreateTeamAndAssignLeagueResponse()
 *
 * 를 Team마다 호출한다.
 *
 * 따라서 Team 하나당 하나의 Transaction이라는
 * 실제 애플리케이션 동작을 검증하기 위해
 * Test 자체 Transaction을 비활성화한다.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FootballSyncLeagueTeamsServiceTest {


    @Autowired
    FootballSyncService footballSyncService;


    @Autowired
    TeamRepository teamRepository;


    /*
     * syncLeagueTeams() 자체에서 직접 사용하는 Repository는 아니지만
     * Test Transaction을 비활성화했으므로
     * 테스트 간 데이터 격리를 위해 함께 정리한다.
     *
     * FK 자식부터 삭제하기 위해 필요하다.
     */
    @Autowired
    PlayerRepository playerRepository;


    @Autowired
    TeamPlayerRepository teamPlayerRepository;


    @Autowired
    TransferRepository transferRepository;


    /*
     * 실제 API-Football은 호출하지 않는다.
     *
     * 테스트에서
     *
     * - 정상 응답
     * - 빈 응답
     * - 외부 API 실패
     * - 중간 Team 데이터 오류
     *
     * 를 직접 만든다.
     */
    @MockitoBean
    ApiFootballHttpClient footballRestClient;


    private final ObjectMapper objectMapper =
            new ObjectMapper();


    @BeforeEach
    void clearDatabase() {

        /*
         * FK 자식 Entity부터 삭제한다.
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
     * 가장 기본적인 syncLeagueTeams() 정상 동작.
     *
     *
     * 상황:
     *
     * API-Football에서 EPL Team으로
     *
     * Arsenal
     * Chelsea
     *
     * 를 반환한다.
     *
     *
     * 현재 리팩토링 구조:
     *
     * FootballSyncService.syncLeagueTeams()
     *
     *      ↓
     *
     * 외부 API 호출
     *
     *      ↓
     *
     * Arsenal
     *
     * FootballSyncTxService
     * .getOrCreateTeamAndAssignLeagueResponse()
     *
     *      ↓
     *
     * Team 생성
     * leagueCode = EPL
     * TeamItemResponseDto 생성
     *
     *      ↓
     *
     * COMMIT
     *
     *
     * Chelsea도 동일한 별도 Transaction으로 처리한다.
     *
     *
     * 최종적으로 Service에서는
     *
     * LeagueTeamsResponseDto
     *
     * 를 반환한다.
     */
    @Test
    @DisplayName(
            "리그 팀을 동기화하면 Team이 저장되고 leagueCode가 반영된 응답을 반환한다."
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
        LeagueTeamsResponseDto response =
                footballSyncService
                        .syncLeagueTeams(
                                leagueCode
                        );


        // then

        /*
         * [신규 - 응답 DTO 검증]
         *
         * 기존에는 String을 반환했지만
         * 현재는 LeagueTeamsResponseDto를 반환한다.
         */
        assertThat(response.getLeagueCode())
                .isEqualTo(
                        LeagueCode.EPL
                );


        assertThat(response.getTeams())
                .hasSize(2);


        assertThat(response.getTeams())
                .extracting(
                        TeamItemResponseDto::getTeamName
                )
                .containsExactly(
                        "Arsenal",
                        "Chelsea"
                );


        /*
         * TeamItemResponseDto 생성 역시
         * TxService 내부에서 완료된다.
         */
        assertThat(response.getTeams())
                .extracting(
                        TeamItemResponseDto::getLogoUrl
                )
                .containsExactly(
                        "https://media.api-sports.io/football/teams/42.png",
                        "https://media.api-sports.io/football/teams/49.png"
                );


        /*
         * 실제 DB 상태 검증.
         */
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


        /*
         * DTO의 teamId는 API-Football ID가 아니라
         * 우리 DB Team PK다.
         *
         * 실제 저장된 Team PK와 같은지 검증한다.
         */
        assertThat(response.getTeams().get(0).getTeamId())
                .isEqualTo(
                        arsenal.getId()
                );


        assertThat(response.getTeams().get(1).getTeamId())
                .isEqualTo(
                        chelsea.getId()
                );
    }


    /*
     * [기존 유지 - 중요]
     *
     * 외부 API 호출 동안에는
     * DB Transaction이 없어야 한다.
     *
     * Transaction은 API 응답을 받은 이후
     * Team을 하나씩 처리하면서 시작된다.
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
                     * true라면 외부 API 응답을 기다리는 동안에도
                     * DB Transaction을 잡고 있다는 뜻이다.
                     *
                     * 현재 설계에서는 false가 정상이다.
                     */
                    assertThat(
                            TransactionSynchronizationManager
                                    .isActualTransactionActive()
                    )
                            .isFalse();


                    return apiResponse;
                });


        // when
        LeagueTeamsResponseDto response =
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


        assertThat(response.getLeagueCode())
                .isEqualTo(
                        LeagueCode.EPL
                );


        assertThat(response.getTeams())
                .hasSize(2);


        assertThat(teamRepository.count())
                .isEqualTo(2);
    }


    /*
     * [기존 수정 - 중요]
     *
     * 동일한 리그 Team Sync가 반복되어도
     * Team을 중복 생성하면 안 된다.
     *
     *
     * 상황:
     *
     * 동일 EPL 응답으로
     * syncLeagueTeams()를 두 번 실행한다.
     *
     *
     * 기대 결과:
     *
     * API 호출 = 2회
     *
     * DB Team = 2건 유지
     *
     * 기존 Team PK 유지
     *
     * 응답 DTO도 두 번째 요청에서 정상 반환
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
        LeagueTeamsResponseDto firstResponse =
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


        LeagueTeamsResponseDto secondResponse =
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
         * 기존 Entity가 실제 재사용됐는지
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


        /*
         * 첫 번째 / 두 번째 요청 모두
         * 정상적인 DTO를 반환해야 한다.
         */
        assertThat(firstResponse.getTeams())
                .hasSize(2);


        assertThat(secondResponse.getTeams())
                .hasSize(2);


        assertThat(secondResponse.getLeagueCode())
                .isEqualTo(
                        LeagueCode.EPL
                );


        assertThat(secondResponse.getTeams())
                .extracting(
                        TeamItemResponseDto::getTeamName
                )
                .containsExactly(
                        "Arsenal",
                        "Chelsea"
                );


        then(footballRestClient)
                .should(times(2))
                .callExternalLeagueTeamsApi(
                        leagueCode
                );
    }


    /*
     * [기존 수정]
     *
     * 상황:
     *
     * Arsenal이 이미 DB에 존재한다.
     *
     * 하지만 아직 leagueCode가 없다.
     *
     *
     * API-Football에서 EPL Team 목록을 다시 받는다.
     *
     *
     * 기대 결과:
     *
     * 기존 Arsenal 재사용
     *
     * leagueCode = EPL 반영
     *
     * Chelsea 신규 생성
     *
     * +
     *
     * 두 Team의 DTO 반환
     */
    @Test
    @DisplayName(
            "이미 존재하는 Team은 재사용하고 leagueCode를 반영해 응답한다."
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
        LeagueTeamsResponseDto response =
                footballSyncService
                        .syncLeagueTeams(
                                LeagueCode.EPL
                        );


        // then
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
         * 기존 Entity를 재사용했는지 검증한다.
         */
        assertThat(foundArsenal.getId())
                .isEqualTo(
                        existingTeamId
                );


        /*
         * Transaction 안에서 assignTeamLeague()가 실행되고
         * Commit 시 Dirty Checking으로 반영되어야 한다.
         */
        assertThat(foundArsenal.getLeagueCode())
                .isEqualTo(
                        LeagueCode.EPL
                );


        /*
         * 기존 Entity를 사용한 경우에도
         * DTO 변환이 Tx 안에서 정상적으로 수행되어야 한다.
         */
        assertThat(response.getLeagueCode())
                .isEqualTo(
                        LeagueCode.EPL
                );


        assertThat(response.getTeams())
                .hasSize(2);


        assertThat(response.getTeams())
                .extracting(
                        TeamItemResponseDto::getTeamName
                )
                .containsExactly(
                        "Arsenal",
                        "Chelsea"
                );


        assertThat(response.getTeams().get(0).getTeamId())
                .isEqualTo(
                        existingTeamId
                );
    }


    /*
     * [기존 수정 - 경계]
     *
     * API 호출은 성공했지만
     * response 배열이 비어있는 상황.
     *
     *
     * Team을 처리하는 반복문 자체가 실행되지 않으므로
     * DB 변경은 없다.
     *
     * 하지만 syncLeagueTeams() 자체는 정상 종료되고
     *
     * leagueCode = EPL
     * teams = []
     *
     * 인 DTO를 반환한다.
     */
    @Test
    @DisplayName(
            "리그 팀 응답이 비어있으면 빈 Team 목록을 반환하고 DB는 변경하지 않는다."
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
        LeagueTeamsResponseDto response =
                footballSyncService
                        .syncLeagueTeams(
                                leagueCode
                        );


        // then
        assertThat(response.getLeagueCode())
                .isEqualTo(
                        LeagueCode.EPL
                );


        assertThat(response.getTeams())
                .isEmpty();


        assertThat(teamRepository.count())
                .isZero();
    }


    // ==================================================
    // 예외 상황
    // ==================================================


    /*
     * [기존 유지]
     *
     * API-Football 호출 자체에서 예외가 발생한다.
     *
     * 아직 FootballSyncTxService에 진입하지 않았으므로
     * DB 변경은 없어야 한다.
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
     * [기존 수정 - 중요]
     *
     * 현재 syncLeagueTeams()의 Transaction 단위를 검증한다.
     *
     *
     * 현재 구조:
     *
     * Arsenal
     *
     *      ↓
     *
     * getOrCreateTeamAndAssignLeagueResponse()
     *
     *      ↓
     *
     * Transaction #1
     *
     * Team 생성
     * leagueCode 지정
     * DTO 생성
     *
     *      ↓
     *
     * COMMIT
     *
     *
     * Chelsea
     *
     *      ↓
     *
     * Transaction #2
     *
     * 잘못된 Team 데이터 처리
     *
     *      ↓
     *
     * ROLLBACK
     *
     *
     * Liverpool은 Chelsea 예외가
     * Service까지 전파되므로 실행되지 않는다.
     *
     *
     * 기대 결과:
     *
     * Arsenal   = 유지
     * Chelsea   = 없음
     * Liverpool = 처리 안 됨
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
         * 첫 번째 Team Transaction은 이미 Commit됐다.
         *
         * DTO 생성까지 정상적으로 완료된 뒤
         * 다음 Team으로 넘어간 상태다.
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
         * getOrCreateTeam()의 신규 Team 생성 과정에서
         * 예외가 발생하므로 해당 Transaction이 Rollback된다.
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
         * 두 번째 Team의 예외 때문에
         * 반복이 중단되어 처리되지 않는다.
         */
        assertThat(
                teamRepository
                        .findByApiFootballId(
                                40L
                        )
        )
                .isEmpty();


        assertThat(teamRepository.count())
                .isEqualTo(1);
    }


    // ==================================================
    // Fixture
    // ==================================================


    /*
     * [Fixture - 정상 EPL Team 목록]
     *
     * Arsenal
     * Chelsea
     *
     * 두 Team을 반환한다.
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
     * [Fixture - 빈 리그 Team 목록]
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
     * [Fixture - 두 번째 Team 데이터 오류]
     *
     * Arsenal
     *      → 정상
     *
     * Chelsea
     *      → id만 있고 name 없음
     *      → 예외
     *
     * Liverpool
     *      → 정상 데이터지만 처리되지 않음
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