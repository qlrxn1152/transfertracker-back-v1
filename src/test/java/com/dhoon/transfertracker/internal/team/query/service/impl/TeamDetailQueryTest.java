package com.dhoon.transfertracker.internal.team.query.service.impl;

import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.exception.InvalidTeamIdException;
import com.dhoon.transfertracker.internal.team.exception.NotFoundTeamException;
import com.dhoon.transfertracker.internal.team.query.dto.response.TeamItemResponseDto;
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
        TeamQueryOrchestrationService.class,
        TeamQueryTxService.class
})
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TeamDetailQueryTest {


    @Autowired
    TeamQueryOrchestrationService teamQueryOrchestrationService;

    @Autowired
    TeamRepository teamRepository;


    @AfterEach
    void clearDatabase() {
        teamRepository.deleteAll();
    }


    // ==================================================
    // 정상
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
                teamQueryOrchestrationService.getTeam(
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


    @Test
    @DisplayName("한글 팀명이 존재하면 displayName으로 한글 팀명을 반환한다.")
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
                teamQueryOrchestrationService.getTeam(
                        arsenal.getId()
                );


        // then
        assertThat(response.getTeamId())
                .isEqualTo(
                        arsenal.getId()
                );

        assertThat(response.getTeamName())
                .isEqualTo(
                        "아스널"
                );

        assertThat(response.getLogoUrl())
                .isEqualTo(
                        "https://media.api-sports.io/football/teams/42.png"
                );
    }


    @Test
    @DisplayName("한글 팀명이 없으면 기존 영문 팀명을 반환한다.")
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
                teamQueryOrchestrationService.getTeam(
                        manUnited.getId()
                );


        // then
        assertThat(response.getTeamName())
                .isEqualTo(
                        "Manchester United"
                );
    }


    // ==================================================
    // 실패
    // ==================================================

    @Test
    @DisplayName("teamId가 null이면 예외가 발생한다.")
    void getTeam_nullTeamId() {

        // when & then
        assertThatThrownBy(() ->
                teamQueryOrchestrationService.getTeam(
                        null
                )
        )
                .isInstanceOf(
                        InvalidTeamIdException.class
                );
    }


    @Test
    @DisplayName("존재하지 않는 팀을 조회하면 예외가 발생한다.")
    void getTeam_notFound() {

        // given
        Long teamId = 999999L;


        // when & then
        assertThatThrownBy(() ->
                teamQueryOrchestrationService.getTeam(
                        teamId
                )
        )
                .isInstanceOf(
                        NotFoundTeamException.class
                );
    }
}