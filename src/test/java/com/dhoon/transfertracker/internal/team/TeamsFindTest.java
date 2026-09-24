package com.dhoon.transfertracker.internal.team;

import com.dhoon.transfertracker.integrate.TeamCreateFixture;
import com.dhoon.transfertracker.internal.domain.Team;
import com.dhoon.transfertracker.internal.dto.team.TeamItemResponseDto;
import com.dhoon.transfertracker.internal.dto.team.TeamsResponseDto;
import com.dhoon.transfertracker.internal.repository.TeamRepository;
import com.dhoon.transfertracker.internal.service.TeamService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class TeamsFindTest {

    @Autowired TeamService teamService;

    @Autowired TeamCreateFixture teamCreateFixture;


    @Test
    @DisplayName(value = "등록되어져 있는 팀이 존재하지 않으면, 빈 리스트를 반환한다.")
    void getTeams_size_zero() throws Exception {
        // given
        teamCreateFixture.saveTeams(0);

        // when
        TeamsResponseDto response = teamService.getTeams();

        // then
        assertThat(response.getTeams()).isEmpty();
    }

    @Test
    @DisplayName(value = "등록되어져 있는 팀들을 조회할 수 있다.")
    void getTeams() throws Exception {
        // given
        teamCreateFixture.saveTeams(20);

        // when
        TeamsResponseDto response = teamService.getTeams();

        // then
        assertThat(response.getTeams()).hasSize(20);
        assertThat(response.getTeams()).extracting(TeamItemResponseDto::getTeamName)
                .contains("team0", "team1", "team2", "team18", "team14");
    }

    @Test
    @DisplayName(value = "등록되어져 있는 특정 팀을 조회할 수 있다.")
    void getTeam() throws Exception {
        // given
        List<Team> teams = teamCreateFixture.saveTeams(20);

        // when
        TeamItemResponseDto team = teamService.getTeam(teams.get(0).getId());

        // then
        assertThat(team.getTeamName()).isEqualTo("team0");
    }

}
