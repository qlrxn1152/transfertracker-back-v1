package com.dhoon.transfertracker.internal.team.query.controller;

import com.dhoon.transfertracker.internal.member.repository.MemberRepository;
import com.dhoon.transfertracker.internal.team.exception.InvalidTeamIdException;
import com.dhoon.transfertracker.internal.team.exception.NotFoundTeamException;
import com.dhoon.transfertracker.internal.team.exception.TeamErrorCode;
import com.dhoon.transfertracker.internal.team.query.dto.response.record.TeamItemResponseDto;
import com.dhoon.transfertracker.internal.team.query.service.impl.TeamQueryOrchestrationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
                .andExpect(jsonPath("$.code").value(TeamErrorCode.TEAM_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value(TeamErrorCode.TEAM_NOT_FOUND.getMessage()));

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
                .andExpect(jsonPath("$.code").value(TeamErrorCode.INVALID_TEAM_ID.getCode()))
                .andExpect(jsonPath("$.message").value(TeamErrorCode.INVALID_TEAM_ID.getMessage()));

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










}
