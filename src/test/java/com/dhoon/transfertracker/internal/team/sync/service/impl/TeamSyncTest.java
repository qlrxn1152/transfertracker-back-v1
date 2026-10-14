package com.dhoon.transfertracker.internal.team.sync.service.impl;

import com.dhoon.transfertracker.external.football.client.ApiFootballHttpClient;
import com.dhoon.transfertracker.external.football.dto.TeamSaveResponseDto;
import com.dhoon.transfertracker.internal.team.domain.Team;
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
class TeamSyncTest {


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
    @DisplayName("팀 정보를 동기화하면 새로운 Team이 저장된다.")
    void syncTeam_success() {

        // given
        Long teamApiId = 42L;

        JsonNode apiResponse =
                teamResponse(
                        42L,
                        "Arsenal"
                );


        given(
                apiFootballHttpClient
                        .callExternalTeamApi(
                                teamApiId
                        )
        )
                .willReturn(
                        apiResponse
                );


        // when
        TeamSaveResponseDto response =
                teamSyncOrchestrationService
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


    // ==================================================
    // Transaction Boundary
    // ==================================================

    @Test
    @DisplayName("팀 외부 API를 호출하는 동안에는 DB 트랜잭션이 활성화되어 있지 않다.")
    void syncTeam_externalApiCall_withoutTransaction() {

        // given
        Long teamApiId = 42L;

        JsonNode apiResponse =
                teamResponse(
                        42L,
                        "Arsenal"
                );


        given(
                apiFootballHttpClient
                        .callExternalTeamApi(
                                teamApiId
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
        teamSyncOrchestrationService
                .syncTeam(
                        teamApiId
                );


        // then
        then(apiFootballHttpClient)
                .should(times(1))
                .callExternalTeamApi(
                        teamApiId
                );


        assertThat(teamRepository.count())
                .isEqualTo(1);
    }


    // ==================================================
    // 멱등성
    // ==================================================

    @Test
    @DisplayName("동일한 팀을 여러 번 동기화해도 Team을 중복 저장하지 않는다.")
    void syncTeam_sameTeam_idempotent() {

        // given
        Long teamApiId = 42L;

        JsonNode apiResponse =
                teamResponse(
                        42L,
                        "Arsenal"
                );


        given(
                apiFootballHttpClient
                        .callExternalTeamApi(
                                teamApiId
                        )
        )
                .willReturn(
                        apiResponse
                );


        // when
        TeamSaveResponseDto firstResponse =
                teamSyncOrchestrationService
                        .syncTeam(
                                teamApiId
                        );


        TeamSaveResponseDto secondResponse =
                teamSyncOrchestrationService
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


        assertThat(firstResponse.getTeamAPIId())
                .isEqualTo(
                        42L
                );

        assertThat(secondResponse.getTeamAPIId())
                .isEqualTo(
                        42L
                );


        then(apiFootballHttpClient)
                .should(times(2))
                .callExternalTeamApi(
                        teamApiId
                );
    }


    // ==================================================
    // 기존 Entity 재사용
    // ==================================================

    @Test
    @DisplayName("이미 존재하는 팀은 새로 저장하지 않고 기존 Team을 재사용한다.")
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
                apiFootballHttpClient
                        .callExternalTeamApi(
                                42L
                        )
        )
                .willReturn(
                        apiResponse
                );


        // when
        TeamSaveResponseDto response =
                teamSyncOrchestrationService
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


    @Test
    @DisplayName("기존 Team에 한글 이름이 있으면 Sync 후에도 기존 displayName을 반환한다.")
    void syncTeam_existingTeamWithKoreanName_preserve() {

        // given
        Team arsenal =
                Team.of(
                        "Arsenal",
                        42L
                );

        arsenal.assignKoTeamName(
                "아스널"
        );

        Team existingTeam =
                teamRepository.save(
                        arsenal
                );


        given(
                apiFootballHttpClient
                        .callExternalTeamApi(
                                42L
                        )
        )
                .willReturn(
                        teamResponse(
                                42L,
                                "Arsenal"
                        )
                );


        // when
        TeamSaveResponseDto response =
                teamSyncOrchestrationService
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


        assertThat(foundTeam.getId())
                .isEqualTo(
                        existingTeam.getId()
                );

        assertThat(foundTeam.getTeamNameKo())
                .isEqualTo(
                        "아스널"
                );


        assertThat(response.getTeamName())
                .isEqualTo(
                        "아스널"
                );
    }


    // ==================================================
    // 외부 API 실패
    // ==================================================

    @Test
    @DisplayName("팀 외부 API 호출에 실패하면 Team 데이터는 변경되지 않는다.")
    void syncTeam_externalApiFail_noDatabaseChange() {

        // given
        Long teamApiId = 42L;


        given(
                apiFootballHttpClient
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
                teamSyncOrchestrationService
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


        then(apiFootballHttpClient)
                .should(times(1))
                .callExternalTeamApi(
                        teamApiId
                );
    }


    // ==================================================
    // Fixture
    // ==================================================

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
                """.formatted(
                        teamApiId,
                        teamName
                )
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