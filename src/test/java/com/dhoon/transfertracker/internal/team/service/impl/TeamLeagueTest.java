package com.dhoon.transfertracker.internal.team.service.impl;

import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.dto.response.TeamItemResponseDto;
import com.dhoon.transfertracker.internal.team.dto.response.TeamsResponseDto;
import com.dhoon.transfertracker.internal.team.exception.InvalidLeagueCodeValueException;
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
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TeamLeagueTest {

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
    @DisplayName("선택한 리그에 속한 팀만 조회할 수 있다.")
    void getLeagueTeams_success() {

        // given
        Team arsenal =
                teamRepository.save(
                        Team.of(
                                "Arsenal",
                                42L,
                                LeagueCode.EPL
                        )
                );

        Team manUnited =
                teamRepository.save(
                        Team.of(
                                "Manchester United",
                                33L,
                                LeagueCode.EPL
                        )
                );

        teamRepository.save(
                Team.of(
                        "Barcelona",
                        529L,
                        LeagueCode.LA_LIGA
                )
        );


        // when
        TeamsResponseDto response =
                teamOrchestrationService.getLeagueTeams(
                        LeagueCode.EPL
                );


        // then
        assertThat(response.getTeams())
                .hasSize(2);

        assertThat(response.getTeams())
                .extracting(
                        TeamItemResponseDto::getTeamId
                )
                .containsExactlyInAnyOrder(
                        arsenal.getId(),
                        manUnited.getId()
                );

        assertThat(response.getTeams())
                .extracting(
                        TeamItemResponseDto::getTeamName
                )
                .containsExactlyInAnyOrder(
                        "Arsenal",
                        "Manchester United"
                );
    }


    @Test
    @DisplayName("선택한 리그가 아닌 팀은 조회되지 않는다.")
    void getLeagueTeams_excludeOtherLeague() {

        // given
        teamRepository.save(
                Team.of(
                        "Arsenal",
                        42L,
                        LeagueCode.EPL
                )
        );

        teamRepository.save(
                Team.of(
                        "Barcelona",
                        529L,
                        LeagueCode.LA_LIGA
                )
        );


        // when
        TeamsResponseDto response =
                teamOrchestrationService.getLeagueTeams(
                        LeagueCode.EPL
                );


        // then
        assertThat(response.getTeams())
                .hasSize(1);

        assertThat(response.getTeams())
                .extracting(
                        TeamItemResponseDto::getTeamName
                )
                .containsExactly(
                        "Arsenal"
                );

        assertThat(response.getTeams())
                .extracting(
                        TeamItemResponseDto::getTeamName)
                .doesNotContain(
                        "Barcelona"
                );
    }


    /*
     * getLeagueTeams() 역시
     * TeamItemResponseDto.of()
     *      ↓
     * Team.getDisplayName()
     *
     * 을 사용한다.
     */
    @Test
    @DisplayName("리그별 팀 조회에서도 한글 팀명이 있으면 한글 이름을 반환한다.")
    void getLeagueTeams_withKoreanName() {

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
                teamOrchestrationService.getLeagueTeams(
                        LeagueCode.EPL
                );


        // then
        assertThat(response.getTeams())
                .hasSize(1);

        assertThat(response.getTeams().get(0).getTeamName())
                .isEqualTo(
                        "아스널"
                );
    }


    // ==================================================
    // 경계 상황
    // ==================================================

    @Test
    @DisplayName("선택한 리그에 팀이 없으면 빈 목록을 반환한다.")
    void getLeagueTeams_empty() {

        // given
        teamRepository.save(
                Team.of(
                        "Arsenal",
                        42L,
                        LeagueCode.EPL
                )
        );


        // when
        TeamsResponseDto response =
                teamOrchestrationService.getLeagueTeams(
                        LeagueCode.LA_LIGA
                );


        // then
        assertThat(response.getTeams())
                .isEmpty();
    }


    // ==================================================
    // 실패 상황
    // ==================================================

    /*
     * null LeagueCode는 DB까지 내려가지 않고
     * OrchestrationService에서 차단된다.
     */
    @Test
    @DisplayName("leagueCode가 null이면 예외가 발생한다.")
    void getLeagueTeams_nullLeagueCode() {

        // when & then
        assertThatThrownBy(() ->
                teamOrchestrationService.getLeagueTeams(
                        null
                )
        )
                .isInstanceOf(
                        InvalidLeagueCodeValueException.class
                );
    }
}