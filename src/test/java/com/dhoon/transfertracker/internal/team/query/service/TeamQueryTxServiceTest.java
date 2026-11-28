package com.dhoon.transfertracker.internal.team.query.service;

import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.exception.InvalidLeagueCodeValueException;
import com.dhoon.transfertracker.internal.team.exception.InvalidTeamIdException;
import com.dhoon.transfertracker.internal.team.exception.NotFoundTeamException;
import com.dhoon.transfertracker.internal.team.query.dto.response.record.TeamDetailResponseDto;
import com.dhoon.transfertracker.internal.team.query.dto.response.record.TeamItemResponseDto;
import com.dhoon.transfertracker.internal.team.query.dto.response.record.TeamsResponseDto;
import com.dhoon.transfertracker.internal.team.query.service.impl.TeamQueryTxService;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.teamplayer.domain.TeamPlayer;
import com.dhoon.transfertracker.internal.teamplayer.repository.TeamPlayerRepository;
import com.dhoon.transfertracker.internal.post.domain.Post;
import com.dhoon.transfertracker.internal.post.domain.PostJournalist;
import com.dhoon.transfertracker.internal.post.domain.PostTeam;
import com.dhoon.transfertracker.internal.post.repository.PostTeamRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class TeamQueryTxServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamPlayerRepository teamPlayerRepository;

    @Mock
    private PostTeamRepository postTeamRepository;

    @InjectMocks
    private TeamQueryTxService teamQueryTxService;

    @Test
    @DisplayName(value = "존재하는 팀 ID로 조회하면 팀 정보를 반환한다.")
    void getTeam_success() throws Exception {
        // given
        Team team = Team.createTeam("testTeam", 123L);
        ReflectionTestUtils.setField(team, "id", 1L);

        given(teamRepository.findById(1L))
                .willReturn(Optional.of(team));

        // when
        TeamItemResponseDto response = teamQueryTxService.getTeam(1L);

        // then
        assertThat(response.teamName()).isEqualTo("testTeam");
        assertThat(response.teamId()).isEqualTo(1L);
        verify(teamRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName(value = "존재하지 않는 팀 ID로 조회하면 예외가 발생한다.")
    void getTeam_notFound() throws Exception {
        // given
        given(teamRepository.findById(999L))
                .willReturn(Optional.empty());

        // when && then
        assertThatThrownBy(() -> teamQueryTxService.getTeam(999L))
                .isInstanceOf(NotFoundTeamException.class);

        verify(teamRepository, times(1)).findById(999L);
    }

    // teamId = null , 0 , -n 인경우 테스트.
    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0L, -1L})
    @DisplayName(value = "teamId 의 값이 null, 0 , 음수인경우 팀 조회에 실패한다.")
    void getTeam_invalidTeamId(Long teamId) throws Exception {

        // when && then
        assertThatThrownBy(() -> teamQueryTxService.getTeam(teamId))
                .isInstanceOf(InvalidTeamIdException.class);

        verifyNoInteractions(teamRepository);
    }


    @Test
    @DisplayName(value = "리그에 속한 팀들을 조회할 수 있다.")
    void getTeams_success() throws Exception {
        // given
        Team teamA = Team.createTeam("teamA", 123L);
        Team teamB = Team.createTeam("teamB", 124L);
        Team teamC = Team.createTeam("teamC", 125L);

        teamA.assignTeamLeague(LeagueCode.EPL);
        teamB.assignTeamLeague(LeagueCode.EPL);
        teamC.assignTeamLeague(LeagueCode.LA_LIGA);

        Team teamD = Team.createTeam("teamD", 111L);

        given(teamRepository.findAllByLeagueCodeIsNotNull())
                .willReturn(List.of(teamA, teamB, teamC));

        // when
        TeamsResponseDto response = teamQueryTxService.getTeams();

        // then
        assertThat(response.teams()).hasSize(3);
        assertThat(response.teams()).extracting(TeamItemResponseDto::teamName)
                .containsExactlyInAnyOrder("teamA", "teamB", "teamC");

        verify(teamRepository, times(1)).findAllByLeagueCodeIsNotNull();
    }

    @Test
    @DisplayName(value = "리그에 속한 팀들이 존재하지 않으면, 빈 리스트를 반환한다.")
    void getTeams_empty() throws Exception {
        // given
        given(teamRepository.findAllByLeagueCodeIsNotNull())
                .willReturn(List.of());

        // when
        TeamsResponseDto response = teamQueryTxService.getTeams();

        // then
        assertThat(response.teams()).isEmpty();
        verify(teamRepository, times(1)).findAllByLeagueCodeIsNotNull();
    }




    @Test
    @DisplayName(value = "지원하는 리그내, 특정 리그에 속한 팀들을 조회할 수 있다.")
    void getLeagueTeams_success() throws Exception {
        // given
        Team teamA = Team.createTeam("teamA", 123L);
        Team teamB = Team.createTeam("teamB", 124L);
        Team teamC = Team.createTeam("teamC", 125L);

        teamA.assignTeamLeague(LeagueCode.EPL);
        teamB.assignTeamLeague(LeagueCode.EPL);
        teamC.assignTeamLeague(LeagueCode.LA_LIGA);

        given(teamRepository.findAllByLeagueCode(LeagueCode.EPL))
                .willReturn(List.of(teamA, teamB));

        // when
        TeamsResponseDto response = teamQueryTxService.getLeagueTeams(LeagueCode.EPL);

        // then
        assertThat(response.teams()).hasSize(2);
        assertThat(response.teams()).extracting(TeamItemResponseDto::teamName)
                .containsExactlyInAnyOrder("teamA", "teamB");
        verify(teamRepository, times(1)).findAllByLeagueCode(LeagueCode.EPL);
    }

    @Test
    @DisplayName(value = "특정 리그에 속한 팀이 존재하지않으면 빈 리스트를 반환한다.")
    void getLeagueTeams_empty() throws Exception {
        // given
        Team teamC = Team.createTeam("teamC", 125L);
        teamC.assignTeamLeague(LeagueCode.LA_LIGA);

        given(teamRepository.findAllByLeagueCode(LeagueCode.EPL))
                .willReturn(List.of());

        // when
        TeamsResponseDto response = teamQueryTxService.getLeagueTeams(LeagueCode.EPL);

        // then
        assertThat(response.teams()).isEmpty();
        verify(teamRepository, times(1)).findAllByLeagueCode(LeagueCode.EPL);
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
    @DisplayName(value = "지원하지 않는 리그이거나, Null 을 입력한경우 예외가 발생한다.")
    void getLeagueTeams_notSupportLeagues_AndNull(LeagueCode leagueCode) throws Exception {
        // when
        assertThatThrownBy(() -> teamQueryTxService.getLeagueTeams(leagueCode))
                .isInstanceOf(InvalidLeagueCodeValueException.class);

        verifyNoInteractions(teamRepository);
    }





    @Test
    @DisplayName("팀이 존재하면 팀 상세 정보를 반환한다.")
    void getTeamInfo_success() {
        // given
        Pageable pageable = PageRequest.of(1, 1);

        Team team = Team.createTeam("testTeam", 111L);
        ReflectionTestUtils.setField(team, "id", 1L);

        PostTeam teamTransferPost = PostTeam.postAssignTeam(
                team,
                Post.createPost(
                        PostJournalist.FABRIZIO_ROMANO,
                        "content-1",
                        "external-post-id-1",
                        false
                )
        );

        TeamPlayer teamPlayer = TeamPlayer.of(team, Player.of("testPlayer", 999L));
        Page<PostTeam> postPage = new PageImpl<>(
                List.of(teamTransferPost),
                pageable,
                5L
        );


        given(teamRepository.findById(1L))
                .willReturn(Optional.of(team));
        given(teamPlayerRepository.findAllByTeamIdWithLazyEntity(1L))
                .willReturn(List.of(teamPlayer));
        given(postTeamRepository.findAllByTeamId(1L, pageable))
                .willReturn(postPage);

        // when
        TeamDetailResponseDto response = teamQueryTxService.getTeamInfo(1L, pageable);

        // then
        verify(teamRepository, times(1)).findById(1L);
        verify(teamPlayerRepository, times(1)).findAllByTeamIdWithLazyEntity(1L);
        verify(postTeamRepository, times(1)).findAllByTeamId(1L, pageable);
        verify(teamPlayerRepository, never()).countByTeamId(anyLong());

        assertThat(response.teams().teamId()).isEqualTo(1L);
        assertThat(response.teams().teamName()).isEqualTo("testTeam");

        assertThat(response.players().getTeamPlayers()).hasSize(1);
        assertThat(response.players().getTeamPlayers().getFirst().getPlayerName()).isEqualTo("testPlayer");
        assertThat(response.players().getTeamPlayerCount()).isEqualTo(1L);

        assertThat(response.posts().getPosts()).hasSize(1);
        assertThat(response.posts().getPosts().getFirst().getExternalPostId()).isEqualTo("external-post-id-1");
    }

    @Test
    @DisplayName(value = "존재하지 않는 팀의 상세 정보를 조회하면 예외가 발생한다.")
    void getTeamInfo_notFound() {
        // given
        given(teamRepository.findById(1L))
                .willReturn(Optional.empty());
        PageRequest pageable = PageRequest.of(0, 20);

        // when && then
        assertThatThrownBy(() -> teamQueryTxService.getTeamInfo(1L, pageable))
                .isInstanceOf(NotFoundTeamException.class);

        verify(teamRepository, times(1)).findById(1L);
        verifyNoInteractions(teamPlayerRepository);
        verifyNoInteractions(postTeamRepository);
    }


    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0L, -2L})
    @DisplayName("팀 ID가 null 또는 0 이하이면 상세 정보 조회에 실패한다.")
    void getTeamInfo_invalidTeamId(Long teamId) {
        // given
        Pageable pageable = PageRequest.of(0, 20);

        // when & then
        assertThatThrownBy(() -> teamQueryTxService.getTeamInfo(teamId, pageable))
                .isInstanceOf(InvalidTeamIdException.class);

        verifyNoInteractions(
                teamRepository,
                teamPlayerRepository,
                postTeamRepository
        );
    }

    @Test
    @DisplayName("Pageable이 null이면 팀 상세 정보 조회에 실패한다.")
    void getTeamInfo_nullPageable() {
        // when & then
        assertThatThrownBy(() -> teamQueryTxService.getTeamInfo(1L, null)).isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(
                teamRepository,
                teamPlayerRepository,
                postTeamRepository
        );
    }

    @Test
    @DisplayName("페이징 정보가 없으면 팀 상세 정보 조회에 실패한다.")
    void getTeamInfo_unpagedPageable() {
        // given
        Pageable pageable = Pageable.unpaged();

        // when & then
        assertThatThrownBy(() -> teamQueryTxService.getTeamInfo(1L, pageable))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(
                teamRepository,
                teamPlayerRepository,
                postTeamRepository
        );
    }

    @Test
    @DisplayName("선수단과 게시물이 없는 팀도 상세 정보를 반환한다.")
    void getTeamInfo_emptyPlayersAndPosts() {
        // given
        Long teamId = 1L;
        Pageable pageable = PageRequest.of(0, 20);

        Team team = Team.createTeam("testTeam", 111L);
        ReflectionTestUtils.setField(team, "id", teamId);

        given(teamRepository.findById(teamId))
                .willReturn(Optional.of(team));

        given(teamPlayerRepository.findAllByTeamIdWithLazyEntity(teamId))
                .willReturn(List.of());

        given(postTeamRepository.findAllByTeamId(teamId, pageable))
                .willReturn(Page.empty(pageable));

        // when
        TeamDetailResponseDto response = teamQueryTxService.getTeamInfo(teamId, pageable);

        // then
        assertThat(response.teams().teamId()).isEqualTo(teamId);
        assertThat(response.teams().teamName()).isEqualTo("testTeam");

        assertThat(response.players().getTeamPlayers()).isEmpty();
        assertThat(response.players().getTeamPlayerCount()).isZero();

        assertThat(response.posts().getPosts()).isEmpty();

        verify(teamRepository).findById(teamId);
        verify(teamPlayerRepository).findAllByTeamIdWithLazyEntity(teamId);
        verify(postTeamRepository).findAllByTeamId(teamId, pageable);
    }






}
