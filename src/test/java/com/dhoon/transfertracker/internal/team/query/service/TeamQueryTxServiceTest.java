package com.dhoon.transfertracker.internal.team.query.service;

import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.exception.NotFoundTeamException;
import com.dhoon.transfertracker.internal.team.query.dto.response.TeamItemResponseDto;
import com.dhoon.transfertracker.internal.team.query.dto.response.TeamPageInfosResponseDto;
import com.dhoon.transfertracker.internal.team.query.dto.response.TeamsResponseDto;
import com.dhoon.transfertracker.internal.team.query.service.impl.TeamQueryTxService;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.teamplayer.domain.TeamPlayer;
import com.dhoon.transfertracker.internal.teamplayer.repository.TeamPlayerRepository;
import com.dhoon.transfertracker.internal.transfer.repository.TransferRepository;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPost;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.transferpost.repository.TransferPostRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TeamQueryTxServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamPlayerRepository teamPlayerRepository;

    @Mock
    private TransferPostRepository transferPostRepository;

    @InjectMocks
    private TeamQueryTxService teamQueryTxService;

    @Test
    @DisplayName(value = "존재하는 팀 ID로 조회하면 팀 정보를 반환한다.")
    void getTeam_success() throws Exception {
        // given
        Team team = Team.of("testTeam", 123L);
        ReflectionTestUtils.setField(team, "id", 1L);

        when(teamRepository.findById(1L))
                .thenReturn(Optional.of(team));

        // when
        TeamItemResponseDto response = teamQueryTxService.getTeam(1L);

        // then
        assertThat(response.getTeamName()).isEqualTo("testTeam");
        assertThat(response.getTeamId()).isEqualTo(1L);
        verify(teamRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName(value = "존재하지 않는 팀 ID로 조회하면 NotFoundTeamException이 발생한다.")
    void getTeam_notFound() throws Exception {
        // given
        when(teamRepository.findById(999L))
                .thenReturn(Optional.empty());

        // when && then
        assertThatThrownBy(() -> teamQueryTxService.getTeam(999L))
                .isInstanceOf(NotFoundTeamException.class);
        verify(teamRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName(value = "리그에 속한 팀들을 조회할 수 있다.")
    void getTeams_success() throws Exception {
        // given
        Team teamA = Team.of("teamA", 123L);
        Team teamB = Team.of("teamB", 124L);
        Team teamC = Team.of("teamC", 125L);

        teamA.assignTeamLeague(LeagueCode.EPL);
        teamB.assignTeamLeague(LeagueCode.EPL);
        teamC.assignTeamLeague(LeagueCode.LA_LIGA);

        Team teamD = Team.of("teamD", 111L);

        when(teamRepository.findAllByLeagueCodeIsNotNull())
                .thenReturn(List.of(teamA, teamB, teamC));

        // when
        TeamsResponseDto response = teamQueryTxService.getTeams();

        // then
        assertThat(response.getTeams()).hasSize(3);
        assertThat(response.getTeams()).extracting(TeamItemResponseDto::getTeamName)
                .containsExactlyInAnyOrder("teamA", "teamB", "teamC");
        verify(teamRepository, times(1)).findAllByLeagueCodeIsNotNull();
    }

    @Test
    @DisplayName(value = "리그에 속한 팀들이 존재하지 않으면, 빈 리스트를 반환한다.")
    void getTeams_empty() throws Exception {
        // given
        when(teamRepository.findAllByLeagueCodeIsNotNull())
                .thenReturn(List.of());

        // when
        TeamsResponseDto response = teamQueryTxService.getTeams();

        // then
        assertThat(response.getTeams()).isEmpty();
        verify(teamRepository, times(1)).findAllByLeagueCodeIsNotNull();
    }

    @Test
    @DisplayName(value = "특정 리그에 속한 팀들을 조회할 수 있다.")
    void getLeagueTeams_success() throws Exception {
        // given
        Team teamA = Team.of("teamA", 123L);
        Team teamB = Team.of("teamB", 124L);
        Team teamC = Team.of("teamC", 125L);

        teamA.assignTeamLeague(LeagueCode.EPL);
        teamB.assignTeamLeague(LeagueCode.EPL);
        teamC.assignTeamLeague(LeagueCode.LA_LIGA);

        when(teamRepository.findAllByLeagueCode(LeagueCode.EPL))
                .thenReturn(List.of(teamA, teamB));

        // when
        TeamsResponseDto response = teamQueryTxService.getLeagueTeams(LeagueCode.EPL);

        // then
        assertThat(response.getTeams()).hasSize(2);
        assertThat(response.getTeams()).extracting(TeamItemResponseDto::getTeamName)
                .containsExactlyInAnyOrder("teamA", "teamB");
        verify(teamRepository, times(1)).findAllByLeagueCode(LeagueCode.EPL);
    }

    @Test
    @DisplayName(value = "특정 리그에 속한 팀이 존재하지않으면 빈 리스트를 반환한다.")
    void getLeagueTeams_empty() throws Exception {
        // given
        Team teamC = Team.of("teamC", 125L);
        teamC.assignTeamLeague(LeagueCode.LA_LIGA);

        when(teamRepository.findAllByLeagueCode(LeagueCode.EPL))
                .thenReturn(List.of());

        // when
        TeamsResponseDto response = teamQueryTxService.getLeagueTeams(LeagueCode.EPL);

        // then
        assertThat(response.getTeams()).isEmpty();
        verify(teamRepository, times(1)).findAllByLeagueCode(LeagueCode.EPL);
    }

    @Test
    @DisplayName("팀이 존재하면 팀 상세 정보를 반환한다.")
    void getTeamInfo_success() {
        // given
        Team team = Team.of("testTeam", 111L);
        ReflectionTestUtils.setField(team, "id", 1L);

        TransferPost transferPost1 = TransferPost.of(
                TransferPostSource.FABRIZIO_ROMANO,
                "content-1",
                "external-post-id-1",
                Instant.parse("2026-01-01T12:00:00Z"),
                false,
                "photoUrl-1"
        );

        Player player = Player.of("testPlayer", 999L);

        TeamPlayer teamPlayer = TeamPlayer.of(team, player);


        given(teamRepository.findById(1L))
                .willReturn(Optional.of(team));
        given(teamPlayerRepository.countByTeamId(1L))
                .willReturn(1L);
        given(teamPlayerRepository.findAllByTeamIdWithLazyEntity(1L))
                .willReturn(List.of(teamPlayer));
        given(transferPostRepository.findAllByTeamId(1L))
                .willReturn(List.of(transferPost1));

        // when
        TeamPageInfosResponseDto response = teamQueryTxService.getTeamInfo(1L);

        // then
        verify(teamRepository, times(1)).findById(1L);
        verify(teamPlayerRepository, times(1)).countByTeamId(1L);
        verify(teamPlayerRepository, times(1)).findAllByTeamIdWithLazyEntity(1L);
        verify(transferPostRepository, times(1)).findAllByTeamId(1L);

        assertThat(response.getTeams().getTeamId()).isEqualTo(1L);
        assertThat(response.getTeams().getTeamName()).isEqualTo("testTeam");
        assertThat(response.getTeams().getTeamPlayerCount()).isEqualTo(1L);

        assertThat(response.getPlayers().getTeamPlayers()).hasSize(1);
        assertThat(response.getPlayers().getTeamPlayers().getFirst().getPlayerName()).isEqualTo("testPlayer");

        assertThat(response.getPosts().getPosts()).hasSize(1);
        assertThat(response.getPosts().getPosts().getFirst().getExternalPostId()).isEqualTo("external-post-id-1");
    }

    @Test
    @DisplayName(value = "존재하지 않는 팀의 상세 정보를 조회하면 예외가 발생한다.")
    void getTeamInfo_notFound() {
        // given
        given(teamRepository.findById(1L))
                .willReturn(Optional.empty());

        // when && then
        assertThatThrownBy(() -> teamQueryTxService.getTeamInfo(1L))
                .isInstanceOf(NotFoundTeamException.class);

        verify(teamRepository, times(1)).findById(1L);
        verifyNoInteractions(teamPlayerRepository);
        verifyNoInteractions(transferPostRepository);
    }


}
