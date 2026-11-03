package com.dhoon.transfertracker.internal.team.tx.service;

import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.query.dto.response.TeamItemResponseDto;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.team.sync.dto.response.TeamSaveResponseDto;
import com.dhoon.transfertracker.internal.team.sync.service.impl.TeamSyncTxService;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class TeamSyncTxServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final JsonNode teamData = objectMapper.readTree(
            """
            {
              "id": 123,
              "name": "testTeam"
            }
            """
    );

    @Mock
    private TeamRepository teamRepository;

    @InjectMocks
    private TeamSyncTxService teamSyncTxService;

    @Test
    @DisplayName(value = "해당 팀이 존재하는경우, 해당 팀을 조회 후 반환한다.")
    void getOrCreateTeam_success() throws Exception {
        // given
        Team team = Team.of("testTeam", 123L);

        given(teamRepository.findByApiFootballId(123L))
                .willReturn(Optional.of(team));

        // when
        TeamSaveResponseDto response = teamSyncTxService.getOrCreateTeamResponse(teamData);

        // then
        assertThat(response.getTeamName()).isEqualTo("testTeam");
        assertThat(response.getTeamAPIId()).isEqualTo(123L);
        verify(teamRepository, times(1)).findByApiFootballId(123L);
        verify(teamRepository, never()).save(any(Team.class));
    }

    @Test
    @DisplayName(value = "해당 팀이 존재하지 않는경우, 팀을 생성해서 저장한 이후 반환한다.")
    void getOrCreateTeam_success_notExistTeam() throws Exception {
        // given
        given(teamRepository.findByApiFootballId(123L))
                .willReturn(Optional.empty());

        given(teamRepository.save(any(Team.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        // when
        TeamSaveResponseDto response = teamSyncTxService.getOrCreateTeamResponse(teamData);

        ArgumentCaptor<Team> captor = ArgumentCaptor.forClass(Team.class);

        // then
        assertThat(response.getTeamName()).isEqualTo("testTeam");
        assertThat(response.getTeamAPIId()).isEqualTo(123L);
        verify(teamRepository, times(1)).findByApiFootballId(123L);
        verify(teamRepository, times(1)).save(captor.capture());

        Team savedTeam = captor.getValue();

        assertThat(savedTeam.getApiFootballId()).isEqualTo(123L);
        assertThat(savedTeam.getTeamName()).isEqualTo("testTeam");
    }

    @Test
    @DisplayName(value = "해당 팀이 존재하는경우, 특정 리그 소속으로 할당한다.")
    void getOrCreateTeamAndAssignLeague_success() throws Exception {
        // given
        Team team = Team.of("testTeam", 123L);
        given(teamRepository.findByApiFootballId(123L))
                .willReturn(Optional.of(team));
        ReflectionTestUtils.setField(team, "id", 1L);

        // when
        TeamItemResponseDto response = teamSyncTxService.getOrCreateTeamAndAssignLeagueResponse(teamData, LeagueCode.EPL);

        // then
        assertThat(response.getTeamName()).isEqualTo("testTeam");
        assertThat(response.getTeamId()).isEqualTo(1L);
        assertThat(team.getLeagueCode()).isEqualTo(LeagueCode.EPL);

        verify(teamRepository, times(1)).findByApiFootballId(123L);
        verify(teamRepository, never()).save(any(Team.class));
    }

    @Test
    @DisplayName(value = "해당 팀이 존재하지 않은경우, 팀을 생성하고 해당 팀을 특정 리그 소속으로 할당한다.")
    void getOrCreateTeamAndAssignLeague_success_notFoundTeam() throws Exception {
        // given
        given(teamRepository.findByApiFootballId(123L))
                .willReturn(Optional.empty());

        given(teamRepository.save(any(Team.class)))
                .willAnswer(invocation -> {
                    Team team = invocation.getArgument(0);

                    ReflectionTestUtils.setField(team, "id", 1L);

                    return team;
                });

        // when
        TeamItemResponseDto response = teamSyncTxService.getOrCreateTeamAndAssignLeagueResponse(teamData, LeagueCode.EPL);

        // then
        ArgumentCaptor<Team> captor = ArgumentCaptor.forClass(Team.class);

        verify(teamRepository, times(1)).save(captor.capture());
        verify(teamRepository, times(1)).findByApiFootballId(123L);

        Team savedTeam = captor.getValue();

        assertThat(response.getTeamName()).isEqualTo("testTeam");
        assertThat(response.getTeamId()).isEqualTo(1L);

        assertThat(savedTeam.getDisplayName()).isEqualTo("testTeam");
        assertThat(savedTeam.getId()).isEqualTo(1L);
        assertThat(savedTeam.getLeagueCode()).isEqualTo(LeagueCode.EPL);
    }




}
