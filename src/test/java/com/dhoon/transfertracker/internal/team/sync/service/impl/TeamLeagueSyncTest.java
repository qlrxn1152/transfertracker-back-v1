package com.dhoon.transfertracker.internal.team.sync.service.impl;

import com.dhoon.transfertracker.external.football.client.ApiFootballHttpClient;
import com.dhoon.transfertracker.external.football.dto.LeagueTeamsResponseDto;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.query.dto.response.TeamItemResponseDto;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import org.junit.jupiter.api.AfterEach;
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
        TeamSyncOrchestrationService.class,
        TeamSyncTxService.class
})
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TeamLeagueSyncTest {


    @Autowired
    TeamSyncOrchestrationService teamSyncOrchestrationService;

    @Autowired
    TeamRepository teamRepository;


    @MockitoBean
    ApiFootballHttpClient apiFootballHttpClient;


    private final ObjectMapper objectMapper =
            new ObjectMapper();


    @AfterEach
    void clearDatabase() {
        teamRepository.deleteAll();
    }


    // ==================================================
    // 정상
    // ==================================================

    @Test
    @DisplayName("리그 팀을 동기화하면 Team이 저장되고 leagueCode가 반영된다.")
    void syncLeagueTeams_success() {

        // given
        LeagueCode leagueCode =
                LeagueCode.EPL;


        given(
                apiFootballHttpClient
                        .callExternalLeagueTeamsApi(
                                leagueCode
                        )
        )
                .willReturn(
                        leagueTeamsResponse()
                );


        // when
        LeagueTeamsResponseDto response =
                teamSyncOrchestrationService
                        .syncLeagueTeams(
                                leagueCode
                        );


        // then
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


        assertThat(response.getTeams())
                .extracting(
                        TeamItemResponseDto::getLogoUrl
                )
                .containsExactly(
                        "https://media.api-sports.io/football/teams/42.png",
                        "https://media.api-sports.io/football/teams/49.png"
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


        /*
         * TeamItemResponseDto의 teamId는
         * API-Football ID가 아니라 DB PK다.
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


    // ==================================================
    // Transaction Boundary
    // ==================================================

    @Test
    @DisplayName("리그 팀 외부 API를 호출하는 동안에는 DB 트랜잭션이 활성화되어 있지 않다.")
    void syncLeagueTeams_externalApiCall_withoutTransaction() {

        // given
        LeagueCode leagueCode =
                LeagueCode.EPL;


        JsonNode apiResponse =
                leagueTeamsResponse();


        given(
                apiFootballHttpClient
                        .callExternalLeagueTeamsApi(
                                leagueCode
                        )
        )
                .willAnswer(invocation -> {

                    assertThat(
                            TransactionSynchronizationManager
                                    .isActualTransactionActive()
                    )
                            .isFalse();

                    return apiResponse;
                });


        // when
        LeagueTeamsResponseDto response =
                teamSyncOrchestrationService
                        .syncLeagueTeams(
                                leagueCode
                        );


        // then
        then(apiFootballHttpClient)
                .should(times(1))
                .callExternalLeagueTeamsApi(
                        leagueCode
                );


        assertThat(response.getTeams())
                .hasSize(2);

        assertThat(teamRepository.count())
                .isEqualTo(2);
    }


    // ==================================================
    // 멱등성
    // ==================================================

    @Test
    @DisplayName("동일한 리그 팀 목록을 여러 번 동기화해도 Team을 중복 저장하지 않는다.")
    void syncLeagueTeams_sameData_idempotent() {

        // given
        LeagueCode leagueCode =
                LeagueCode.EPL;


        JsonNode apiResponse =
                leagueTeamsResponse();


        given(
                apiFootballHttpClient
                        .callExternalLeagueTeamsApi(
                                leagueCode
                        )
        )
                .willReturn(
                        apiResponse
                );


        // when
        LeagueTeamsResponseDto firstResponse =
                teamSyncOrchestrationService
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
                teamSyncOrchestrationService
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


        assertThat(secondArsenal.getId())
                .isEqualTo(
                        firstArsenalId
                );

        assertThat(secondArsenal.getLeagueCode())
                .isEqualTo(
                        LeagueCode.EPL
                );


        assertThat(firstResponse.getTeams())
                .hasSize(2);

        assertThat(secondResponse.getTeams())
                .hasSize(2);


        then(apiFootballHttpClient)
                .should(times(2))
                .callExternalLeagueTeamsApi(
                        leagueCode
                );
    }


    // ==================================================
    // 기존 Entity 재사용
    // ==================================================

    @Test
    @DisplayName("이미 존재하는 Team은 재사용하고 leagueCode를 반영한다.")
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


        given(
                apiFootballHttpClient
                        .callExternalLeagueTeamsApi(
                                LeagueCode.EPL
                        )
        )
                .willReturn(
                        leagueTeamsResponse()
                );


        // when
        LeagueTeamsResponseDto response =
                teamSyncOrchestrationService
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


        assertThat(foundArsenal.getId())
                .isEqualTo(
                        existingTeamId
                );


        /*
         * 기존 Entity이지만 leagueCode는
         * 현재 Sync 대상 리그로 설정된다.
         *
         * Dirty Checking으로 Commit 시 DB에 반영된다.
         */
        assertThat(foundArsenal.getLeagueCode())
                .isEqualTo(
                        LeagueCode.EPL
                );


        assertThat(response.getTeams())
                .hasSize(2);


        assertThat(response.getTeams().get(0).getTeamId())
                .isEqualTo(
                        existingTeamId
                );
    }


    @Test
    @DisplayName("기존 Team의 한글 이름은 리그 Sync 후에도 유지된다.")
    void syncLeagueTeams_existingKoreanName_preserve() {

        // given
        Team arsenal =
                Team.of(
                        "Arsenal",
                        42L
                );

        arsenal.assignKoTeamName(
                "아스널"
        );

        Team existingArsenal =
                teamRepository.save(
                        arsenal
                );


        given(
                apiFootballHttpClient
                        .callExternalLeagueTeamsApi(
                                LeagueCode.EPL
                        )
        )
                .willReturn(
                        leagueTeamsResponse()
                );


        // when
        LeagueTeamsResponseDto response =
                teamSyncOrchestrationService
                        .syncLeagueTeams(
                                LeagueCode.EPL
                        );


        // then
        Team foundArsenal =
                teamRepository
                        .findByApiFootballId(
                                42L
                        )
                        .orElseThrow();


        assertThat(foundArsenal.getId())
                .isEqualTo(
                        existingArsenal.getId()
                );

        assertThat(foundArsenal.getTeamNameKo())
                .isEqualTo(
                        "아스널"
                );

        assertThat(foundArsenal.getLeagueCode())
                .isEqualTo(
                        LeagueCode.EPL
                );


        assertThat(response.getTeams().get(0).getTeamName())
                .isEqualTo(
                        "아스널"
                );
    }


    // ==================================================
    // 기존 League 변경
    // ==================================================

    @Test
    @DisplayName("기존 Team에 다른 leagueCode가 있어도 현재 Sync 대상 리그로 변경한다.")
    void syncLeagueTeams_existingOtherLeague_changeLeague() {

        // given
        Team arsenal =
                teamRepository.save(
                        Team.of(
                                "Arsenal",
                                42L,
                                LeagueCode.LA_LIGA
                        )
                );


        given(
                apiFootballHttpClient
                        .callExternalLeagueTeamsApi(
                                LeagueCode.EPL
                        )
        )
                .willReturn(
                        leagueTeamsResponse()
                );


        // when
        teamSyncOrchestrationService
                .syncLeagueTeams(
                        LeagueCode.EPL
                );


        // then
        Team foundArsenal =
                teamRepository
                        .findById(
                                arsenal.getId()
                        )
                        .orElseThrow();


        assertThat(foundArsenal.getLeagueCode())
                .isEqualTo(
                        LeagueCode.EPL
                );
    }


    // ==================================================
    // 빈 응답
    // ==================================================

    @Test
    @DisplayName("리그 팀 응답이 비어있으면 빈 Team 목록을 반환하고 DB는 변경하지 않는다.")
    void syncLeagueTeams_emptyResponse_noDatabaseChange() {

        // given
        LeagueCode leagueCode =
                LeagueCode.EPL;


        given(
                apiFootballHttpClient
                        .callExternalLeagueTeamsApi(
                                leagueCode
                        )
        )
                .willReturn(
                        leagueTeamsEmptyResponse()
                );


        // when
        LeagueTeamsResponseDto response =
                teamSyncOrchestrationService
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
    // 외부 API 실패
    // ==================================================

    @Test
    @DisplayName("리그 팀 외부 API 호출에 실패하면 Team 데이터는 변경되지 않는다.")
    void syncLeagueTeams_externalApiFail_noDatabaseChange() {

        // given
        LeagueCode leagueCode =
                LeagueCode.EPL;


        given(
                apiFootballHttpClient
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
                teamSyncOrchestrationService
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


        then(apiFootballHttpClient)
                .should(times(1))
                .callExternalLeagueTeamsApi(
                        leagueCode
                );
    }


    // ==================================================
    // Team 단위 Transaction
    // ==================================================

    @Test
    @DisplayName("중간 Team 처리에 실패하면 이전 Team은 유지되고 이후 Team은 처리되지 않는다.")
    void syncLeagueTeams_secondTeamFail_partialCommitAndStop() {

        // given
        LeagueCode leagueCode =
                LeagueCode.EPL;


        given(
                apiFootballHttpClient
                        .callExternalLeagueTeamsApi(
                                leagueCode
                        )
        )
                .willReturn(
                        leagueTeamsSecondTeamInvalidResponse()
                );


        // when & then
        assertThatThrownBy(() ->
                teamSyncOrchestrationService
                        .syncLeagueTeams(
                                leagueCode
                        )
        )
                .isInstanceOf(
                        NullPointerException.class
                );


        /*
         * Team #1 - Arsenal
         *
         * 자신의 Transaction에서 정상 처리 후
         * 이미 Commit된 상태다.
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
         * Team #2 - Chelsea
         *
         * name이 없어서 처리 중 실패한다.
         * 해당 Transaction은 Rollback된다.
         */
        assertThat(
                teamRepository
                        .findByApiFootballId(
                                49L
                        )
        )
                .isEmpty();


        /*
         * Team #3 - Liverpool
         *
         * 두 번째 Team의 예외가 전파되면서
         * 반복 자체가 중단되므로 처리되지 않는다.
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


    private JsonNode leagueTeamsEmptyResponse() {

        return readJson(
                """
                {
                  "response": []
                }
                """
        );
    }


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


    private JsonNode readJson(
            String json
    ) {

        try {

            return objectMapper.readTree(
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