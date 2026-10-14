package com.dhoon.transfertracker.internal.team.query.service.impl;

import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.exception.InvalidLeagueCodeValueException;
import com.dhoon.transfertracker.internal.team.query.dto.response.TeamItemResponseDto;
import com.dhoon.transfertracker.internal.team.query.dto.response.TeamsResponseDto;
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
class TeamLeagueQueryTest {


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
    @DisplayName("특정 리그에 속한 팀들을 조회할 수 있다.")
    void getLeagueTeams_success() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                42L,
                LeagueCode.EPL
        );

        Team manUnited = saveTeam(
                "Manchester United",
                33L,
                LeagueCode.EPL
        );

        saveTeam(
                "Barcelona",
                529L,
                LeagueCode.LA_LIGA
        );


        // when
        TeamsResponseDto response =
                teamQueryOrchestrationService.getLeagueTeams(
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
    @DisplayName("선택한 리그가 아닌 팀은 조회하지 않는다.")
    void getLeagueTeams_excludeOtherLeague() {

        // given
        saveTeam(
                "Arsenal",
                42L,
                LeagueCode.EPL
        );

        saveTeam(
                "Barcelona",
                529L,
                LeagueCode.LA_LIGA
        );

        saveTeam(
                "Bayern Munich",
                157L,
                LeagueCode.BUNDESLIGA
        );


        // when
        TeamsResponseDto response =
                teamQueryOrchestrationService.getLeagueTeams(
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
                        TeamItemResponseDto::getTeamName
                )
                .doesNotContain(
                        "Barcelona",
                        "Bayern Munich"
                );
    }


    // ==================================================
    // Display Name
    // ==================================================

    @Test
    @DisplayName("리그별 팀 조회에서도 한글 팀명이 존재하면 displayName을 반환한다.")
    void getLeagueTeams_withKoreanName() {

        // given
        Team arsenal = Team.of(
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
                teamQueryOrchestrationService.getLeagueTeams(
                        LeagueCode.EPL
                );


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
                        "아스널"
                );

        assertThat(team.getLogoUrl())
                .isEqualTo(
                        "https://media.api-sports.io/football/teams/42.png"
                );
    }


    // ==================================================
    // 경계
    // ==================================================

    @Test
    @DisplayName("선택한 리그에 등록된 팀이 없으면 빈 목록을 반환한다.")
    void getLeagueTeams_empty() {

        // given
        saveTeam(
                "Arsenal",
                42L,
                LeagueCode.EPL
        );

        saveTeam(
                "Barcelona",
                529L,
                LeagueCode.LA_LIGA
        );


        // when
        TeamsResponseDto response =
                teamQueryOrchestrationService.getLeagueTeams(
                        LeagueCode.SERIE_A
                );


        // then
        assertThat(response.getTeams())
                .isEmpty();
    }


    @Test
    @DisplayName("리그가 지정되지 않은 팀은 특정 리그 조회 결과에 포함되지 않는다.")
    void getLeagueTeams_excludeTeamWithoutLeague() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                42L,
                LeagueCode.EPL
        );


        teamRepository.save(
                Team.of(
                        "Unknown Team",
                        999L
                )
        );


        // when
        TeamsResponseDto response =
                teamQueryOrchestrationService.getLeagueTeams(
                        LeagueCode.EPL
                );


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
    }


    // ==================================================
    // 실패
    // ==================================================

    @Test
    @DisplayName("leagueCode가 null이면 예외가 발생한다.")
    void getLeagueTeams_nullLeagueCode() {

        // when & then
        assertThatThrownBy(() ->
                teamQueryOrchestrationService.getLeagueTeams(
                        null
                )
        )
                .isInstanceOf(
                        InvalidLeagueCodeValueException.class
                );
    }


    // ==================================================
    // Fixture
    // ==================================================

    private Team saveTeam(
            String teamName,
            Long apiFootballId,
            LeagueCode leagueCode
    ) {

        return teamRepository.save(
                Team.of(
                        teamName,
                        apiFootballId,
                        leagueCode
                )
        );
    }
}