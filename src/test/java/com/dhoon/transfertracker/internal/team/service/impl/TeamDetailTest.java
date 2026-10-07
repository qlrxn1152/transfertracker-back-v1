package com.dhoon.transfertracker.internal.team.service.impl;

import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.dto.response.TeamItemResponseDto;
import com.dhoon.transfertracker.internal.team.exception.InvalidTeamIdException;
import com.dhoon.transfertracker.internal.team.exception.NotFoundTeamException;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@DataJpaTest
@Import({
        TeamOrchestrationService.class,
        TeamTxService.class
})
@ActiveProfiles("test")

/*
 * 실제 요청 구조:
 *
 * Client
 *      ↓
 * Controller
 *      ↓
 * TeamOrchestrationService
 *      → Transaction X
 *      → teamId 검증
 *      ↓
 * TeamTxService
 *      → @Transactional(readOnly = true)
 *      → Repository 조회
 *      → Entity 사용
 *      → DTO 생성
 *      ↓
 * Transaction 종료
 *      ↓
 * TeamOrchestrationService
 *      ↓
 * Controller
 *
 *
 * 실제 Transaction 경계를 검증하기 위해
 * DataJpaTest의 기본 Test Transaction을 사용하지 않는다.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TeamDetailTest {

    /*
     * 실제 요청 흐름의 Service 진입점.
     *
     * TeamTxService를 직접 호출하지 않는다.
     */
    @Autowired
    TeamOrchestrationService teamOrchestrationService;

    @Autowired
    TeamRepository teamRepository;


    /*
     * Test Transaction을 사용하지 않기 때문에
     * 테스트 종료 후 자동 rollback되지 않는다.
     *
     * 따라서 테스트 데이터는 직접 삭제한다.
     */
    @AfterEach
    void clearDatabase() {
        teamRepository.deleteAll();
    }


    // ==================================================
    // 정상 상황
    // ==================================================

    @Test
    @DisplayName("팀 ID를 이용해서 팀 정보를 조회할 수 있다.")
    void getTeam_success() {

        // given
        Team arsenal =
                teamRepository.save(
                        Team.of(
                                "Arsenal",
                                42L,
                                LeagueCode.EPL
                        )
                );


        // when
        TeamItemResponseDto response =
                teamOrchestrationService.getTeam(
                        arsenal.getId()
                );


        // then
        assertThat(response.getTeamId())
                .isEqualTo(
                        arsenal.getId()
                );

        assertThat(response.getTeamName())
                .isEqualTo(
                        "Arsenal"
                );

        assertThat(response.getLogoUrl())
                .isEqualTo(
                        "https://media.api-sports.io/football/teams/42.png"
                );
    }


    /*
     * TeamItemResponseDto는
     *
     * team.getTeamName()
     *
     * 이 아니라
     *
     * team.getDisplayName()
     *
     * 을 사용한다.
     *
     * 따라서 한글 팀명이 존재하면
     * 한글 팀명이 API 응답에 사용되어야 한다.
     */
    @Test
    @DisplayName("한글 팀명이 존재하면 한글 팀명을 반환한다.")
    void getTeam_withKoreanName() {

        // given
        Team arsenal =
                Team.of(
                        "Arsenal",
                        42L,
                        LeagueCode.EPL
                );

        arsenal.assignKoTeamName(
                "아스널"
        );

        arsenal =
                teamRepository.save(
                        arsenal
                );


        // when
        TeamItemResponseDto response =
                teamOrchestrationService.getTeam(
                        arsenal.getId()
                );


        // then
        assertThat(response.getTeamName())
                .isEqualTo(
                        "아스널"
                );
    }


    /*
     * getDisplayName()의 fallback 정책.
     *
     * teamNameKo가 존재하지 않으면
     * 원래 영문 팀명을 사용한다.
     */
    @Test
    @DisplayName("한글 팀명이 없으면 기존 팀명을 반환한다.")
    void getTeam_withoutKoreanName() {

        // given
        Team manUnited =
                teamRepository.save(
                        Team.of(
                                "Manchester United",
                                33L,
                                LeagueCode.EPL
                        )
                );


        // when
        TeamItemResponseDto response =
                teamOrchestrationService.getTeam(
                        manUnited.getId()
                );


        // then
        assertThat(response.getTeamName())
                .isEqualTo(
                        "Manchester United"
                );
    }


    // ==================================================
    // 실패 상황
    // ==================================================

    /*
     * Orchestration 단계의 validation은 통과하지만
     * 실제 DB에 Team이 존재하지 않는 경우.
     *
     * TeamTxService
     *      ↓
     * teamRepository.findById()
     *      ↓
     * NotFoundTeamException
     */
    @Test
    @DisplayName("존재하지 않는 팀을 조회하면 예외가 발생한다.")
    void getTeam_notFound() {

        // given
        Long teamId = 999999L;


        // when & then
        assertThatThrownBy(() ->
                teamOrchestrationService.getTeam(
                        teamId
                )
        )
                .isInstanceOf(
                        NotFoundTeamException.class
                );
    }


    /*
     * Repository까지 내려가기 전에
     * OrchestrationService에서 차단되어야 한다.
     */
    @Test
    @DisplayName("teamId가 null이면 예외가 발생한다.")
    void getTeam_nullTeamId() {

        // when & then
        assertThatThrownBy(() ->
                teamOrchestrationService.getTeam(
                        null
                )
        )
                .isInstanceOf(
                        InvalidTeamIdException.class
                );
    }
}