package com.dhoon.transfertracker.internal.team.query.dto.response.record;

import com.dhoon.transfertracker.internal.team.domain.Team;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TeamItemResponseDtoTest {

    @Test
    @DisplayName(value = "Team Entity 를 응답 DTO 로 변환한다.")
    void convertTeam_toResponseDto() throws Exception {
        // given
        Team team = mock(Team.class);
        when(team.getId()).thenReturn(1L);
        when(team.getDisplayName()).thenReturn("testTeam");
        when(team.getApiFootballId()).thenReturn(42L);

        // when
        TeamItemResponseDto response = TeamItemResponseDto.of(team);

        // then
        assertThat(response.teamId()).isEqualTo(1L);
        assertThat(response.teamName()).isEqualTo("testTeam");
        assertThat(response.logoUrl()).isEqualTo("https://media.api-sports.io/football/teams/42.png");

    }
}
