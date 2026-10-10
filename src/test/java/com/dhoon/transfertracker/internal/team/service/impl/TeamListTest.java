package com.dhoon.transfertracker.internal.team.service.impl;

import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.dto.response.TeamItemResponseDto;
import com.dhoon.transfertracker.internal.team.dto.response.TeamsResponseDto;
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


@DataJpaTest
@Import({
        TeamOrchestrationService.class,
        TeamTxService.class
})
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TeamListTest {

    @Autowired
    TeamOrchestrationService teamOrchestrationService;

    @Autowired
    TeamRepository teamRepository;


    @AfterEach
    void clearDatabase() {
        teamRepository.deleteAll();
    }


    // ==================================================
    // 정상 상황
    // ==================================================

    @Test
    @DisplayName("리그에 등록된 팀 목록을 조회할 수 있다.")
    void getTeams_success() {

        // given
        Team arsenal =
                teamRepository.save(
                        Team.of(
                                "Arsenal",
                                42L,
                                LeagueCode.EPL
                        )
                );

        Team barcelona =
                teamRepository.save(
                        Team.of(
                                "Barcelona",
                                529L,
                                LeagueCode.LA_LIGA
                        )
                );

        Team bayern =
                teamRepository.save(
                        Team.of(
                                "Bayern Munich",
                                157L,
                                LeagueCode.BUNDESLIGA
                        )
                );


        // when
        TeamsResponseDto response =
                teamOrchestrationService.getTeams();


        // then
        assertThat(response.getTeams())
                .hasSize(3);

        assertThat(response.getTeams())
                .extracting(
                        TeamItemResponseDto::getTeamId
                )
                .containsExactlyInAnyOrder(
                        arsenal.getId(),
                        barcelona.getId(),
                        bayern.getId()
                );

        assertThat(response.getTeams())
                .extracting(
                        TeamItemResponseDto::getTeamName
                )
                .containsExactlyInAnyOrder(
                        "Arsenal",
                        "Barcelona",
                        "Bayern Munich"
                );
    }


    @Test
    @DisplayName("팀 조회 결과는 TeamItemResponseDto로 정상 변환된다.")
    void getTeams_mapping() {

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
        TeamsResponseDto response =
                teamOrchestrationService.getTeams();


        // then
        assertThat(response.getTeams())
                .hasSize(1);

        TeamItemResponseDto team =
                response.getTeams().get(0);

        assertThat(team.getTeamId())
                .isEqualTo(
                        arsenal.getId()
                );

        assertThat(team.getTeamName())
                .isEqualTo(
                        "Arsenal"
                );

        assertThat(team.getLogoUrl())
                .isEqualTo(
                        "https://media.api-sports.io/football/teams/42.png"
                );
    }


    @Test
    @DisplayName("팀 조회에서도 한글 팀명이 존재하면 한글 이름을 반환한다.")
    void getTeams_withKoreanName() {

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

        teamRepository.save(
                arsenal
        );


        // when
        TeamsResponseDto response =
                teamOrchestrationService.getTeams();


        // then
        assertThat(response.getTeams())
                .hasSize(1);

        assertThat(response.getTeams().get(0).getTeamName())
                .isEqualTo(
                        "아스널"
                );
    }


    /*
     * 현재 getTeams()의 비즈니스 규칙:
     *
     * findAll()
     *      ↓
     * findAllByLeagueCodeIsNotNull()
     *
     * 따라서 리그가 지정되지 않은 팀은
     * 일반 팀 목록에서 제외되어야 한다.
     */
    @Test
    @DisplayName("리그에 등록되지 않은 팀은 팀 목록에서 제외한다.")
    void getTeams_excludeTeamWithoutLeague() {

        // given
        Team arsenal =
                teamRepository.save(
                        Team.of(
                                "Arsenal",
                                42L,
                                LeagueCode.EPL
                        )
                );

        teamRepository.save(
                Team.of(
                        "Unknown Team",
                        999L
                )
        );


        // when
        TeamsResponseDto response =
                teamOrchestrationService.getTeams();


        // then
        assertThat(response.getTeams())
                .hasSize(1);

        assertThat(response.getTeams())
                .extracting(
                        TeamItemResponseDto::getTeamId
                )
                .containsExactly(
                        arsenal.getId()
                );

        assertThat(response.getTeams())
                .extracting(
                        TeamItemResponseDto::getTeamName
                )
                .containsExactly(
                        "Arsenal"
                );
    }


    // ==================================================
    // 경계 상황
    // ==================================================

    @Test
    @DisplayName("리그에 등록된 팀이 없으면 빈 목록을 반환한다.")
    void getTeams_empty() {

        // when
        TeamsResponseDto response =
                teamOrchestrationService.getTeams();


        // then
        assertThat(response.getTeams())
                .isEmpty();
    }
}