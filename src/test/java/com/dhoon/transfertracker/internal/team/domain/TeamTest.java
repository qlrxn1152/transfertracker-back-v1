package com.dhoon.transfertracker.internal.team.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

public class TeamTest {

    @Test
    @DisplayName(value = "정상적인 팀 정보로 Team 을 생성한다.")
    void createTeam() throws Exception {
        // given
        String teamName = "teamName";
        Long apiFootballId = 1L;

        // when
        Team team = Team.createTeam(teamName, apiFootballId);

        // then
        assertThat(team.getTeamName()).isEqualTo("teamName");
        assertThat(team.getApiFootballId()).isEqualTo(1L);
        assertThat(team.getDisplayName()).isEqualTo("teamName");
        assertThat(team.getTeamNameKo()).isNull();
        assertThat(team.getLeagueCode()).isNull();
    }

    @Test
    @DisplayName(value = "팀 이름의 앞뒤 공백을 제거한 뒤, Team 을 생성한다.")
    void createTeam_stirpTeamName() throws Exception {
        // given
        String teamName = "    teamName  ";
        Long apiFootballId = 1L;

        // when
        Team team = Team.createTeam(teamName, apiFootballId);

        // then
        assertThat(team.getTeamName()).isEqualTo("teamName");
        assertThat(team.getApiFootballId()).isEqualTo(1L);
        assertThat(team.getDisplayName()).isEqualTo("teamName");
        assertThat(team.getTeamNameKo()).isNull();
        assertThat(team.getLeagueCode()).isNull();
    }

    @Test
    @DisplayName(value = "팀 이름의 앞뒤 공백만 제거하고 중간 공백은 제거하지않은채, Team 을 생성한다.")
    void createTeam_stirpTeamName_withoutMiddle() throws Exception {
        // given
        String teamName = "    team Name  ";
        Long apiFootballId = 1L;

        // when
        Team team = Team.createTeam(teamName, apiFootballId);

        // then
        assertThat(team.getTeamName()).isEqualTo("team Name");
        assertThat(team.getApiFootballId()).isEqualTo(1L);
        assertThat(team.getDisplayName()).isEqualTo("team Name");
        assertThat(team.getTeamNameKo()).isNull();
        assertThat(team.getLeagueCode()).isNull();
    }

    @Test
    @DisplayName(value = "팀의 API ID 값이 null 이거나, 0 이하인경우, 팀 생성에 실패한다")
    void createTeam_fail_invalidApiId() throws Exception {
        // given
        String teamName = "teamName";
        Long nullApiFootballId = null;

        Long negativeApiFootballId = -1L;
        Long zeroApiFootballId = 0L;

        // when && then
        assertThatThrownBy(() -> Team.createTeam(teamName, nullApiFootballId))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> Team.createTeam(teamName, negativeApiFootballId))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> Team.createTeam(teamName, zeroApiFootballId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName(value = "teamName 값이 null 이거나, 비어져있거나, 문자열 null인 경우 팀 생성에 실패한다")
    void createTeam_fail_invalidTeamName() throws Exception {
        // given
        String nullTeamName = null;
        String emptyTeamName = "";
        String emptyTeamName2 = "      ";
        String stringNullTeamName = "null";

        Long apiFootballId = 1L;

        // when && then
        assertThatThrownBy(() -> Team.createTeam(nullTeamName, apiFootballId))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> Team.createTeam(emptyTeamName, apiFootballId))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> Team.createTeam(emptyTeamName2, apiFootballId))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> Team.createTeam(stringNullTeamName, apiFootballId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName(value = "정상적인 한국어 이름을 팀에 할당할경우, 한국어 이름 할당에 성공한다.")
    void assign_teamNameKo() throws Exception {
        // given
        String teamName = "teamName";
        Long apiFootballId = 1L;
        Team team = Team.createTeam(teamName, apiFootballId);

        String teamNameKo = "한국어 팀이름";

        // when
        team.assignKoTeamName(teamNameKo);

        // then
        assertThat(team.getTeamName()).isEqualTo("teamName");
        assertThat(team.getApiFootballId()).isEqualTo(1L);

        assertThat(team.getTeamNameKo()).isEqualTo("한국어 팀이름");
        assertThat(team.getDisplayName()).isEqualTo("한국어 팀이름");
    }

    @Test
    @DisplayName(value = "한국어 팀 이름에 앞뒤 공백만 제거한 뒤, 할당한다.")
    void assign_teamNameKo_stirp() throws Exception {
        // given
        String teamName = "teamName";
        Long apiFootballId = 1L;
        Team team = Team.createTeam(teamName, apiFootballId);

        String teamNameKo = "   한국어 팀이름  ";

        // when
        team.assignKoTeamName(teamNameKo);

        // then
        assertThat(team.getTeamName()).isEqualTo("teamName");
        assertThat(team.getApiFootballId()).isEqualTo(1L);

        assertThat(team.getTeamNameKo()).isEqualTo("한국어 팀이름");
        assertThat(team.getDisplayName()).isEqualTo("한국어 팀이름");
    }

    @Test
    @DisplayName(value = "한국어 팀 이름이 존재하는경우, displayerName 은 한국어 팀 이름을 반환한다.")
    void assign_teamNameKo_displayer_KoTeamName() throws Exception {
        // given
        String teamName = "teamName";
        Long apiFootballId = 1L;
        Team team = Team.createTeam(teamName, apiFootballId);

        String teamNameKo = "한국어 팀이름";

        // when
        team.assignKoTeamName(teamNameKo);

        // then
        assertThat(team.getTeamName()).isEqualTo("teamName");
        assertThat(team.getApiFootballId()).isEqualTo(1L);

        assertThat(team.getTeamNameKo()).isEqualTo("한국어 팀이름");
        assertThat(team.getDisplayName()).isEqualTo("한국어 팀이름");
    }

    @Test
    @DisplayName(value = "한국어 팀 이름이 null / empty / 문자열 null 인 경우, 한국어 팀 이름 할당에 실패한다.")
    void assign_teamNameKo_invalidTeamNameKo() throws Exception {
        // given
        String teamName = "teamName";
        Long apiFootballId = 1L;
        Team team = Team.createTeam(teamName, apiFootballId);

        String nullTeamNameKo = null;
        String emptyTeamNameKo = "";
        String stringNullTeamNameKo = "null";

        // when && then
        assertThatThrownBy(() -> team.assignKoTeamName(nullTeamNameKo))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> team.assignKoTeamName(emptyTeamNameKo))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> team.assignKoTeamName(stringNullTeamNameKo))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName(value = "지원하는 리그인경우, 존재하는 팀에대해서 리그할당에 성공한다.")
    void assignLeagueCode() throws Exception {
        // given
        String teamName = "teamName";
        Long apiFootballId = 1L;
        Team team = Team.createTeam(teamName, apiFootballId);

        LeagueCode epl = LeagueCode.EPL;

        // when
        team.assignTeamLeague(epl);

        // then
        assertThat(team.getLeagueCode()).isEqualTo(LeagueCode.EPL);
    }

    @Test
    @DisplayName(value = "리그 코드값에 null 을 입력한경우 리그할당에 실패한다.")
    void assignLeagueCode_fail_null() throws Exception {
        // given
        String teamName = "teamName";
        Long apiFootballId = 1L;
        Team team = Team.createTeam(teamName, apiFootballId);


        // when && then
        assertThatThrownBy(() -> team.assignTeamLeague(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName(value = "지원하지 않는 리그인경우, 리그할당에 실패한다.")
    void assignLeagueCode_fail_notSupportLeague() throws Exception {
        // given
        String teamName = "teamName";
        Long apiFootballId = 1L;
        Team team = Team.createTeam(teamName, apiFootballId);


        // when && then
        assertThatThrownBy(() -> team.assignTeamLeague(LeagueCode.CHAMPIONS_LEAGUE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> team.assignTeamLeague(LeagueCode.WORLD_CUP))
                .isInstanceOf(IllegalArgumentException.class);
    }






}
