package com.dhoon.transfertracker.internal.team.query.service;

import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.exception.InvalidLeagueCodeValueException;
import com.dhoon.transfertracker.internal.team.exception.InvalidTeamIdException;
import com.dhoon.transfertracker.internal.team.query.dto.response.TeamItemResponseDto;
import com.dhoon.transfertracker.internal.team.query.dto.response.TeamPageInfoForTeamResponseDto;
import com.dhoon.transfertracker.internal.team.query.dto.response.TeamPageInfosResponseDto;
import com.dhoon.transfertracker.internal.team.query.dto.response.TeamsResponseDto;
import com.dhoon.transfertracker.internal.team.query.service.impl.TeamQueryOrchestrationService;
import com.dhoon.transfertracker.internal.team.query.service.impl.TeamQueryTxService;
import com.dhoon.transfertracker.internal.teamplayer.domain.TeamPlayer;
import com.dhoon.transfertracker.internal.teamplayer.query.dto.response.TeamPlayerItemResponseDto;
import com.dhoon.transfertracker.internal.teamplayer.query.dto.response.TeamPlayersResponseDto;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPost;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostTeam;
import com.dhoon.transfertracker.internal.transferpost.dto.response.TransferPostItemResponseDto;
import com.dhoon.transfertracker.internal.transferpost.dto.response.TransferPostsResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class TeamQueryOrchestrationServiceTest {

    @Mock
    private TeamQueryTxService txService;

    @InjectMocks
    private TeamQueryOrchestrationService orchestrationService;

    @Test
    @DisplayName(value = "존재하는 팀 ID 에 대해서, 해당 팀 조회에 성공한다.")
    void getTeam() throws Exception {
        // given
        String teamName = "testTeam";
        Long teamApiId = 111L;
        Long teamId = 1L;
        Team team = Team.of(teamName, teamApiId);
        ReflectionTestUtils.setField(team, "id", teamId);

        TeamItemResponseDto expect = TeamItemResponseDto.of(team);

        given(txService.getTeam(1L))
                .willReturn(expect);

        // when
        TeamItemResponseDto response = orchestrationService.getTeam(1L);

        // then
        assertThat(response).isEqualTo(expect);

        assertThat(response.getTeamId()).isEqualTo(1L);
        assertThat(response.getTeamName()).isEqualTo("testTeam");

        verify(txService, times(1)).getTeam(1L);
    }

    @Test
    @DisplayName(value = "팀 ID 가 null 이거나, 0 이하인 경우, 예외가 발생한다.")
    void getTeam_invalid_teamId() throws Exception {
        // given
        String teamName = "testTeam";
        Long teamApiId = 111L;
        Team team = Team.of(teamName, teamApiId);
        ReflectionTestUtils.setField(team, "id", 1L);

        Long nullTeamId = null;
        Long zeroTeamId = 0L;
        Long negativeTeamId = -1L;

        // when && then
        assertThatThrownBy(() -> orchestrationService.getTeam(nullTeamId))
                .isInstanceOf(InvalidTeamIdException.class);

        assertThatThrownBy(() -> orchestrationService.getTeam(zeroTeamId))
                .isInstanceOf(InvalidTeamIdException.class);

        assertThatThrownBy(() -> orchestrationService.getTeam(negativeTeamId))
                .isInstanceOf(InvalidTeamIdException.class);

        verifyNoInteractions(txService);
    }

    @Test
    @DisplayName(value = "존재하는 리그에 속한 모든팀들을 조회할 수 있다.")
    void getTeams() throws Exception {
        // given
        Team t1 = Team.of("team-1", 111L);
        t1.assignTeamLeague(LeagueCode.LA_LIGA);

        Team t2 = Team.of("team-2", 222L);
        t2.assignTeamLeague(LeagueCode.EPL);

        Team t3 = Team.of("team-3", 333L);

        TeamsResponseDto expect = TeamsResponseDto.of(List.of(
                TeamItemResponseDto.of(t1),
                TeamItemResponseDto.of(t2)
        ));

        given(txService.getTeams())
                .willReturn(expect);

        // when
        TeamsResponseDto response = orchestrationService.getTeams();

        // then
        assertThat(response).isEqualTo(expect);

        assertThat(response.getTeams()).hasSize(2);
        assertThat(response.getTeams()).extracting(TeamItemResponseDto::getTeamName)
                .containsExactlyInAnyOrder("team-1", "team-2");

        verify(txService, times(1)).getTeams();
    }

    @Test
    @DisplayName(value = "리그에 존재하는 팀이 없을경우, 빈 리스트를 반환한다.")
    void getTeams_empty() throws Exception {
        // given
        Team t1 = Team.of("team-1", 111L);
        Team t2 = Team.of("team-2", 222L);
        Team t3 = Team.of("team-3", 333L);

        TeamsResponseDto expect = TeamsResponseDto.of(List.of());

        given(txService.getTeams())
                .willReturn(expect);

        // when
        TeamsResponseDto response = orchestrationService.getTeams();

        // then
        assertThat(response).isEqualTo(expect);
        assertThat(response.getTeams()).isEmpty();

        verify(txService, times(1)).getTeams();
    }

    @Test
    @DisplayName(value = "특정 리그에 존재하는 팀만 조회할 수 있다.")
    void getLeagueTeams() throws Exception {
        // given
        Team t1 = Team.of("team-1", 111L);
        Team t2 = Team.of("team-2", 222L);
        t1.assignTeamLeague(LeagueCode.LA_LIGA);
        t2.assignTeamLeague(LeagueCode.LA_LIGA);

        Team t3 = Team.of("team-3", 333L);
        t3.assignTeamLeague(LeagueCode.K_LEAGUE);

        TeamsResponseDto expect = TeamsResponseDto.of(List.of(
                TeamItemResponseDto.of(t1),
                TeamItemResponseDto.of(t2)
        ));

        given(txService.getLeagueTeams(LeagueCode.LA_LIGA))
                .willReturn(expect);

        // when
        TeamsResponseDto response = orchestrationService.getLeagueTeams(LeagueCode.LA_LIGA);

        // then
        assertThat(response).isEqualTo(expect);

        assertThat(response.getTeams()).hasSize(2);
        assertThat(response.getTeams()).extracting(TeamItemResponseDto::getTeamName)
                .containsExactlyInAnyOrder("team-1", "team-2");

        verify(txService, times(1)).getLeagueTeams(LeagueCode.LA_LIGA);
    }

    @Test
    @DisplayName(value = "특정 리그에 존재하는 팀이 없을경우, 빈 리스트를 반환한다.")
    void getLeagueTeams_empty() throws Exception {
        // given
        Team t1 = Team.of("team-1", 111L);
        Team t2 = Team.of("team-2", 222L);
        t1.assignTeamLeague(LeagueCode.LA_LIGA);
        t2.assignTeamLeague(LeagueCode.LA_LIGA);

        Team t3 = Team.of("team-3", 333L);
        t3.assignTeamLeague(LeagueCode.K_LEAGUE);

        TeamsResponseDto expect = TeamsResponseDto.of(List.of());

        given(txService.getLeagueTeams(LeagueCode.EPL))
                .willReturn(expect);

        // when
        TeamsResponseDto response = orchestrationService.getLeagueTeams(LeagueCode.EPL);

        // then
        assertThat(response).isEqualTo(expect);
        assertThat(response.getTeams()).isEmpty();

        verify(txService, times(1)).getLeagueTeams(LeagueCode.EPL);
    }

    @Test
    @DisplayName(value = "리그코드가 null 인 경우, 예외가 발생한다")
    void getLeagueTeams_invalid_leagueCode() throws Exception {
        // when && then
        assertThatThrownBy(() -> orchestrationService.getLeagueTeams(null))
                .isInstanceOf(InvalidLeagueCodeValueException.class);

        verifyNoInteractions(txService);
    }

    @Test
    @DisplayName(value = "존재하는 팀에대해서, 팀 정보들을 확인할 수 있다.")
    void getTeamInfo() throws Exception {
        // given
        Instant now = Instant.parse("2026-11-08T12:00:00Z");

        Team team = Team.of("testTeam", 111L);
        ReflectionTestUtils.setField(team, "id", 1L);

        TeamPlayer tp1 = TeamPlayer.of(team, Player.of("player-1", 33L));
        TeamPlayer tp2 = TeamPlayer.of(team, Player.of("player-2", 34L));


        TransferPostTeam tpt1 = TransferPostTeam.postAssignTeam(
                team,
                TransferPost.of(
                        TransferPostSource.FABRIZIO_ROMANO,
                        "Romano transfer news 1",
                        "post-100",
                        now.minusSeconds(3600),
                        true
                ));

        TransferPostTeam tpt2 = TransferPostTeam.postAssignTeam(
                team,
                TransferPost.of(
                        TransferPostSource.FABRIZIO_ROMANO,
                        "Romano transfer news 2",
                        "post-101",
                        now,
                        false
                ));

        TeamPageInfosResponseDto expect = TeamPageInfosResponseDto.of(
                TeamPageInfoForTeamResponseDto.of(team, 2),
                TeamPlayersResponseDto.of(List.of(
                        TeamPlayerItemResponseDto.of(tp1),
                        TeamPlayerItemResponseDto.of(tp2)
                )),
                TransferPostsResponseDto.of(
                        List.of(
                                TransferPostItemResponseDto.of(tpt2),
                        TransferPostItemResponseDto.of(tpt1)),
                        2,
                        2,
                        false,
                        false
                )
        );

        int page = 0;

        PageRequest pageable = PageRequest.of(
                page,
                20,
                Sort.by(
                        Sort.Order.desc("post.contentCreatedAt"),
                        Sort.Order.desc("post.id")
                )
        );

        given(txService.getTeamInfo(1L, pageable))
                .willReturn(expect);


        // when
        TeamPageInfosResponseDto response = orchestrationService.getTeamInfo(1L, page);

        // then
        assertThat(response).isEqualTo(expect);

        assertThat(response.getPlayers().getTeamPlayers()).hasSize(2);
        assertThat(response.getPlayers().getTeamPlayers()).extracting(TeamPlayerItemResponseDto::getPlayerName)
                .containsExactlyInAnyOrder("player-1", "player-2");

        assertThat(response.getTeams().getTeamPlayerCount()).isEqualTo(2);
        assertThat(response.getTeams().getTeamId()).isEqualTo(1L);
        assertThat(response.getTeams().getTeamName()).isEqualTo("testTeam");

        assertThat(response.getPosts().getPosts()).hasSize(2);
        assertThat(response.getPosts().isHasNext()).isFalse();
        assertThat(response.getPosts().isHasPrevious()).isFalse();
        assertThat(response.getPosts().getTotalPostCount()).isEqualTo(2L);
        assertThat(response.getPosts().getTotalPageCount()).isEqualTo(2);

        assertThat(response.getPosts().getPosts()).extracting(TransferPostItemResponseDto::getExternalPostId)
                .containsExactly("post-101", "post-100");
    }

    @Test
    @DisplayName(value = "teamId 의 값이 null 이거나, 0 이하인경우 팀 상세 조회시 예외가 발생한다.")
    void getTeamInfo_invalid_teamId() throws Exception {
        // given
        Long nullTeamId = null;
        Long zeroTeamId = 0L;
        Long negativeTeamId = -1L;

        // when && then
        assertThatThrownBy(() -> orchestrationService.getTeamInfo(nullTeamId, 0))
                .isInstanceOf(InvalidTeamIdException.class);

        assertThatThrownBy(() -> orchestrationService.getTeamInfo(zeroTeamId, 0))
                .isInstanceOf(InvalidTeamIdException.class);

        assertThatThrownBy(() -> orchestrationService.getTeamInfo(negativeTeamId, 0))
                .isInstanceOf(InvalidTeamIdException.class);
    }

    @Test
    @DisplayName(value = "page 의 값이 음수인경우, 팀 상세 조회시 예외가 발생한다.")
    void getTeamInfo_invalid_page() throws Exception {
        // given
        Team team = Team.of("teamName", 111L);
        ReflectionTestUtils.setField(team, "id", 1L);

        int negativePage = -1;

        // when && then
        assertThatThrownBy(() -> orchestrationService.getTeamInfo(1L, negativePage))
                .isInstanceOf(IllegalArgumentException.class);
    }


}
