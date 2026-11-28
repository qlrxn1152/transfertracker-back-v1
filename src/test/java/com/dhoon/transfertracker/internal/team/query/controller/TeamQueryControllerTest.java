package com.dhoon.transfertracker.internal.team.query.controller;

import com.dhoon.transfertracker.internal.member.repository.MemberRepository;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.exception.InvalidLeagueCodeValueException;
import com.dhoon.transfertracker.internal.team.exception.InvalidTeamIdException;
import com.dhoon.transfertracker.internal.team.exception.NotFoundTeamException;
import com.dhoon.transfertracker.internal.team.exception.TeamErrorCode;
import com.dhoon.transfertracker.internal.team.query.dto.response.record.TeamItemResponseDto;
import com.dhoon.transfertracker.internal.team.query.dto.response.record.TeamsResponseDto;
import com.dhoon.transfertracker.internal.team.query.service.impl.TeamQueryOrchestrationService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(TeamQueryController.class)
@AutoConfigureMockMvc(addFilters = false)
public class TeamQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TeamQueryOrchestrationService teamQueryOrchestrationService;

    @MockitoBean
    private MemberRepository memberRepository;

    @Test
    @DisplayName(value = "teamId 로 조회하면 HTTP 200 과, 팀 정보를 반환한다.")
    void getTeam_success() throws Exception {
        // given
        Long teamId = 1L;

        TeamItemResponseDto expected = new TeamItemResponseDto(
                teamId,
                "Arsenal",
                "https://media.api-sports.io/football/teams/42.png"
        );

        given(teamQueryOrchestrationService.getTeam(teamId))
                .willReturn(expected);

        // when && then
        mockMvc.perform(get("/api/teams/{teamId}", teamId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teamId").value(1L))
                .andExpect(jsonPath("$.teamName").value("Arsenal"))
                .andExpect(jsonPath("$.logoUrl").value("https://media.api-sports.io/football/teams/42.png"));

        verify(teamQueryOrchestrationService, times(1)).getTeam(teamId);
    }

    @Test
    @DisplayName("존재하지 않는 팀을 조회하면 HTTP 404, 에러정보를 반환한다.")
    void getTeam_notFound() throws Exception {
        // given
        Long teamId = 999L;

        given(teamQueryOrchestrationService.getTeam(teamId))
                .willThrow(new NotFoundTeamException());

        // when && then
        mockMvc.perform(get("/api/teams/{teamId}", teamId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TEAM_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 팀입니다."));

        verify(teamQueryOrchestrationService).getTeam(teamId);
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L})
    @DisplayName(value = "teamId 가 0 / 음수 인경우, 조회시 HTTP 400, 에러정보를 반환한다.")
    void getTeam_invalidTeamId(Long teamId) throws Exception {
        // given
        given(teamQueryOrchestrationService.getTeam(teamId))
                .willThrow(new InvalidTeamIdException());

        // when && then
        mockMvc.perform(get("/api/teams/{teamId}", teamId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TEAM_ID"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 팀입니다."));

        verify(teamQueryOrchestrationService).getTeam(teamId);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "null"})
    @DisplayName("팀 ID의 타입 변환에 실패하면 공통 에러 응답을 반환한다.")
    void getTeam_invalidParameterType(String teamId) throws Exception {
        // when && then
        mockMvc.perform(get("/api/teams/{teamId}", teamId))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER_TYPE"))
                .andExpect(jsonPath("$.message").value("잘못된 파라미터 타입입니다."));

        verifyNoInteractions(teamQueryOrchestrationService);
    }

    @Test
    @DisplayName(value = "팀 목록을 조회하면, HTTP 200과 리그에 속해있는 팀들을 조회할 수 있다.")
    void getTeams() throws Exception {
        // given
        TeamItemResponseDto t1 = new TeamItemResponseDto(1L, "team1", "logo-1");
        TeamItemResponseDto t2 = new TeamItemResponseDto(2L, "team2", "logo-2");

        TeamsResponseDto expected = TeamsResponseDto.of(List.of(t1, t2));

        given(teamQueryOrchestrationService.getTeams())
                .willReturn(expected);

        // when
        mockMvc.perform(get("/api/teams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teams").isArray())
                .andExpect(jsonPath("$.teams.length()").value(2))

                .andExpect(jsonPath("$.teams[0].teamId").value(1L))
                .andExpect(jsonPath("$.teams[0].teamName").value("team1"))
                .andExpect(jsonPath("$.teams[0].logoUrl").value("logo-1"))

                .andExpect(jsonPath("$.teams[1].teamId").value(2L))
                .andExpect(jsonPath("$.teams[1].teamName").value("team2"))
                .andExpect(jsonPath("$.teams[1].logoUrl").value("logo-2"));

        verify(teamQueryOrchestrationService).getTeams();
    }

    @Test
    @DisplayName(value = "리그에 존재하는 팀이 없는경우, 빈 리스트를 반환한다.")
    void getTeams_empty() throws Exception {
        // given
        TeamsResponseDto expected = TeamsResponseDto.of(List.of());

        given(teamQueryOrchestrationService.getTeams())
                .willReturn(expected);

        // when
        mockMvc.perform(get("/api/teams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teams").isArray())
                .andExpect(jsonPath("$.teams").isEmpty());

        verify(teamQueryOrchestrationService).getTeams();
    }


    @Test
    @DisplayName(value = "특정 리그에 존재하는 팀들을 조회할 수 있다.")
    void getLeagueTeams() throws Exception {
        TeamItemResponseDto t1 = new TeamItemResponseDto(1L, "team1", "logo-1");
        TeamItemResponseDto t2 = new TeamItemResponseDto(2L, "team2", "logo-2");

        TeamsResponseDto expected = TeamsResponseDto.of(List.of(t1, t2));

        given(teamQueryOrchestrationService.getLeagueTeams(LeagueCode.EPL))
                .willReturn(expected);

        // when && then
        mockMvc.perform(get("/api/teams/league").param("leagueCode", "EPL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teams").isArray())
                .andExpect(jsonPath("$.teams.length()").value(2))

                .andExpect(jsonPath("$.teams[0].teamId").value(1L))
                .andExpect(jsonPath("$.teams[0].teamName").value("team1"))
                .andExpect(jsonPath("$.teams[0].logoUrl").value("logo-1"))

                .andExpect(jsonPath("$.teams[1].teamId").value(2L))
                .andExpect(jsonPath("$.teams[1].teamName").value("team2"))
                .andExpect(jsonPath("$.teams[1].logoUrl").value("logo-2"));

        verify(teamQueryOrchestrationService).getLeagueTeams(LeagueCode.EPL);
    }

    @Test
    @DisplayName(value = "특정 리그에 존재하는 팀이 존재하지 않는경우, 빈 리스트를 반환한다.")
    void getLeagueTeams_empty() throws Exception {
        // given
        TeamsResponseDto expected = TeamsResponseDto.of(List.of());

        given(teamQueryOrchestrationService.getLeagueTeams(LeagueCode.EPL))
                .willReturn(expected);

        // when && then
        mockMvc.perform(get("/api/teams/league").param("leagueCode", "EPL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teams").isArray())
                .andExpect(jsonPath("$.teams").isEmpty());

        verify(teamQueryOrchestrationService).getLeagueTeams(LeagueCode.EPL);
    }


    @ParameterizedTest
    @EnumSource(
            value = LeagueCode.class,
            names = {"CHAMPIONS_LEAGUE", "EUROPA_LEAGUE", "CONFERENCE_LEAGUE", "FA_CUP", "EFL_CUP", "WORLD_CUP"}
    )
    @DisplayName(value = "지원하지 않는경우 예외가 발생한다.")
    void getLeagueTeam_invalidLeagueCode(LeagueCode leagueCode) throws Exception {
        // given
        given(teamQueryOrchestrationService.getLeagueTeams(leagueCode))
                .willThrow(new InvalidLeagueCodeValueException());

        // when && then
        mockMvc.perform(get("/api/teams/league").param("leagueCode", leagueCode.name()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_LEAGUE_CODE"))
                .andExpect(jsonPath("$.message").value("유효하지 않은 리그 코드입니다."));

        verify(teamQueryOrchestrationService).getLeagueTeams(leagueCode);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABC", "null", "123"})
    @DisplayName("리그 코드의 타입 변환에 실패하면 HTTP 400을 반환한다.")
    void getLeagueTeams_invalidParameterType(String leagueCode) throws Exception {
        // when && then
        mockMvc.perform(get("/api/teams/league").param("leagueCode", leagueCode))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER_TYPE"))
                .andExpect(jsonPath("$.message").value("잘못된 파라미터 타입입니다."));

        verifyNoInteractions(teamQueryOrchestrationService);
    }

    @Test
    @DisplayName("리그 코드가 누락되면 HTTP 400을 반환한다.")
    void getLeagueTeams_missingParameter() throws Exception {
        // when && then
        mockMvc.perform(get("/api/teams/league"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_PARAMETER"))
                .andExpect(jsonPath("$.message").value("필수 파라미터가 누락되었습니다."));

        verifyNoInteractions(teamQueryOrchestrationService);
    }












}
