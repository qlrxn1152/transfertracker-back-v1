package com.dhoon.transfertracker.internal.team.query.service;

import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.exception.InvalidLeagueCodeValueException;
import com.dhoon.transfertracker.internal.team.exception.InvalidTeamIdException;
import com.dhoon.transfertracker.internal.team.query.dto.response.record.TeamDetailResponseDto;
import com.dhoon.transfertracker.internal.team.query.dto.response.record.TeamItemResponseDto;
import com.dhoon.transfertracker.internal.team.query.dto.response.record.TeamSummaryResponseDto;
import com.dhoon.transfertracker.internal.team.query.dto.response.record.TeamsResponseDto;
import com.dhoon.transfertracker.internal.team.query.service.impl.TeamQueryOrchestrationService;
import com.dhoon.transfertracker.internal.team.query.service.impl.TeamQueryTxService;
import com.dhoon.transfertracker.internal.teamplayer.domain.TeamPlayer;
import com.dhoon.transfertracker.internal.teamplayer.query.dto.response.TeamPlayerItemResponseDto;
import com.dhoon.transfertracker.internal.teamplayer.query.dto.response.TeamPlayersResponseDto;
import com.dhoon.transfertracker.internal.post.domain.Post;
import com.dhoon.transfertracker.internal.post.domain.PostJournalist;
import com.dhoon.transfertracker.internal.post.domain.PostTeam;
import com.dhoon.transfertracker.internal.post.dto.response.PostItemResponseDto;
import com.dhoon.transfertracker.internal.post.dto.response.PostsResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
        Team team = Team.createTeam(teamName, teamApiId);
        ReflectionTestUtils.setField(team, "id", teamId);

        TeamItemResponseDto expect = TeamItemResponseDto.of(team);

        given(txService.getTeam(1L))
                .willReturn(expect);

        // when
        TeamItemResponseDto response = orchestrationService.getTeam(1L);

        // then
        assertThat(response).isEqualTo(expect);

        assertThat(response.teamId()).isEqualTo(1L);
        assertThat(response.teamName()).isEqualTo("testTeam");

        verify(txService, times(1)).getTeam(1L);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0L, -1L})
    @DisplayName(value = "팀 Id 가 Null 이거나, 0 이하인경우 예외가 발생한다.")
    void getTeam_invalidTeamId(Long teamId) throws Exception {
        // when && then
        assertThatThrownBy(() -> orchestrationService.getTeam(teamId))
                .isInstanceOf(InvalidTeamIdException.class);

        verifyNoInteractions(txService);
    }








    @Test
    @DisplayName(value = "존재하는 리그에 속한 모든팀들을 조회할 수 있다.")
    void getTeams() throws Exception {
        // given
        Team t1 = Team.createTeam("team-1", 111L);
        t1.assignTeamLeague(LeagueCode.LA_LIGA);

        Team t2 = Team.createTeam("team-2", 222L);
        t2.assignTeamLeague(LeagueCode.EPL);

        Team t3 = Team.createTeam("team-3", 333L);

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

        assertThat(response.teams()).hasSize(2);
        assertThat(response.teams()).extracting(TeamItemResponseDto::teamName)
                .containsExactlyInAnyOrder("team-1", "team-2");

        verify(txService, times(1)).getTeams();
    }

    @Test
    @DisplayName(value = "리그에 존재하는 팀이 없을경우, 빈 리스트를 반환한다.")
    void getTeams_empty() throws Exception {
        // given
        Team t1 = Team.createTeam("team-1", 111L);
        Team t2 = Team.createTeam("team-2", 222L);
        Team t3 = Team.createTeam("team-3", 333L);

        TeamsResponseDto expect = TeamsResponseDto.of(List.of());

        given(txService.getTeams())
                .willReturn(expect);

        // when
        TeamsResponseDto response = orchestrationService.getTeams();

        // then
        assertThat(response).isEqualTo(expect);
        assertThat(response.teams()).isEmpty();

        verify(txService, times(1)).getTeams();
    }





    @Test
    @DisplayName(value = "특정 리그에 존재하는 팀만 조회할 수 있다.")
    void getLeagueTeams() throws Exception {
        // given
        Team t1 = Team.createTeam("team-1", 111L);
        Team t2 = Team.createTeam("team-2", 222L);
        t1.assignTeamLeague(LeagueCode.LA_LIGA);
        t2.assignTeamLeague(LeagueCode.LA_LIGA);

        Team t3 = Team.createTeam("team-3", 333L);
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

        assertThat(response.teams()).hasSize(2);
        assertThat(response.teams()).extracting(TeamItemResponseDto::teamName)
                .containsExactlyInAnyOrder("team-1", "team-2");

        verify(txService, times(1)).getLeagueTeams(LeagueCode.LA_LIGA);
    }

    @Test
    @DisplayName(value = "특정 리그에 존재하는 팀이 없을경우, 빈 리스트를 반환한다.")
    void getLeagueTeams_empty() throws Exception {
        // given
        Team t1 = Team.createTeam("team-1", 111L);
        Team t2 = Team.createTeam("team-2", 222L);
        t1.assignTeamLeague(LeagueCode.LA_LIGA);
        t2.assignTeamLeague(LeagueCode.LA_LIGA);

        Team t3 = Team.createTeam("team-3", 333L);
        t3.assignTeamLeague(LeagueCode.K_LEAGUE);

        TeamsResponseDto expect = TeamsResponseDto.of(List.of());

        given(txService.getLeagueTeams(LeagueCode.EPL))
                .willReturn(expect);

        // when
        TeamsResponseDto response = orchestrationService.getLeagueTeams(LeagueCode.EPL);

        // then
        assertThat(response).isEqualTo(expect);
        assertThat(response.teams()).isEmpty();

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

    @ParameterizedTest
    @EnumSource(
            value = LeagueCode.class,
            names = {
                    "CHAMPIONS_LEAGUE",
                    "EUROPA_LEAGUE",
                    "CONFERENCE_LEAGUE",
                    "FA_CUP",
                    "EFL_CUP",
                    "WORLD_CUP"
            }
    )
    @DisplayName(value = "지원하지 않는 리그인경우, 예외가 발생한다.")
    void getLeagueTeams_notSupportLeagues_AndNull(LeagueCode leagueCode) throws Exception {
        // when
        assertThatThrownBy(() -> orchestrationService.getLeagueTeams(leagueCode))
                .isInstanceOf(InvalidLeagueCodeValueException.class);

        verifyNoInteractions(txService);
    }





    @Test
    @DisplayName(value = "존재하는 팀에대해서, 팀 정보들을 확인할 수 있다.")
    void getTeamInfo() throws Exception {
        // given
        Instant now = Instant.parse("2026-11-08T12:00:00Z");

        Team team = Team.createTeam("testTeam", 111L);
        ReflectionTestUtils.setField(team, "id", 1L);

        TeamPlayer tp1 = TeamPlayer.of(team, Player.of("player-1", 33L));
        TeamPlayer tp2 = TeamPlayer.of(team, Player.of("player-2", 34L));


        PostTeam tpt1 = PostTeam.postAssignTeam(
                team,
                Post.createPost(
                        PostJournalist.FABRIZIO_ROMANO,
                        "Romano transfer news 1",
                        "post-100",
                        true
                ));

        PostTeam tpt2 = PostTeam.postAssignTeam(
                team,
                Post.createPost(
                        PostJournalist.FABRIZIO_ROMANO,
                        "Romano transfer news 2",
                        "post-101",
                        false
                ));

        Page<PostItemResponseDto> postPage = new PageImpl<>(List.of(
                PostItemResponseDto.createTeamPost(tpt2),
                PostItemResponseDto.createTeamPost(tpt1)
        ));

        TeamDetailResponseDto expect = TeamDetailResponseDto.of(
                TeamSummaryResponseDto.of(team),
                TeamPlayersResponseDto.of(List.of(
                        TeamPlayerItemResponseDto.of(tp1),
                        TeamPlayerItemResponseDto.of(tp2)
                )),
                PostsResponseDto.of(postPage)
        );

        int page = 0;

        PageRequest pageable = PageRequest.of(page, 20);

        given(txService.getTeamInfo(1L, pageable))
                .willReturn(expect);


        // when
        TeamDetailResponseDto response = orchestrationService.getTeamInfo(1L, page);

        // then
        assertThat(response).isEqualTo(expect);

        assertThat(response.players().getTeamPlayers()).hasSize(2);
        assertThat(response.players().getTeamPlayers()).extracting(TeamPlayerItemResponseDto::getPlayerName)
                .containsExactlyInAnyOrder("player-1", "player-2");
        assertThat(response.players().getTeamPlayerCount()).isEqualTo(2);

        assertThat(response.teams().teamId()).isEqualTo(1L);
        assertThat(response.teams().teamName()).isEqualTo("testTeam");

        assertThat(response.posts().getPosts()).hasSize(2);
        assertThat(response.posts().isHasNext()).isFalse();
        assertThat(response.posts().isHasPrevious()).isFalse();
        assertThat(response.posts().getTotalPostCount()).isEqualTo(2L);
        assertThat(response.posts().getTotalPageCount()).isEqualTo(1);

        assertThat(response.posts().getPosts()).extracting(PostItemResponseDto::getExternalPostId)
                .containsExactly("post-101", "post-100");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0L, -1L})
    @DisplayName(value = "teamId 의 값이 null 이거나, 0 이하인경우 팀 상세 조회시 예외가 발생한다.")
    void getTeamInfo_invalid_teamId(Long teamId) throws Exception {
        // when && then
        assertThatThrownBy(() -> orchestrationService.getTeamInfo(teamId, 0))
                .isInstanceOf(InvalidTeamIdException.class);

        assertThatThrownBy(() -> orchestrationService.getTeamInfo(teamId, 0))
                .isInstanceOf(InvalidTeamIdException.class);

        assertThatThrownBy(() -> orchestrationService.getTeamInfo(teamId, 0))
                .isInstanceOf(InvalidTeamIdException.class);
    }

    @Test
    @DisplayName(value = "page 의 값이 음수인경우, 팀 상세 조회시 예외가 발생한다.")
    void getTeamInfo_invalid_page() throws Exception {
        // given
        Team team = Team.createTeam("teamName", 111L);
        ReflectionTestUtils.setField(team, "id", 1L);

        int negativePage = -1;

        // when && then
        assertThatThrownBy(() -> orchestrationService.getTeamInfo(1L, negativePage))
                .isInstanceOf(IllegalArgumentException.class);
    }


}
