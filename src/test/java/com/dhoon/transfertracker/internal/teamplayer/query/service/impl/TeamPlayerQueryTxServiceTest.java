package com.dhoon.transfertracker.internal.teamplayer.query.service.impl;

import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.exception.NotFoundTeamException;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.teamplayer.domain.TeamPlayer;
import com.dhoon.transfertracker.internal.teamplayer.query.dto.response.TeamPlayerItemResponseDto;
import com.dhoon.transfertracker.internal.teamplayer.query.dto.response.TeamPlayersResponseDto;
import com.dhoon.transfertracker.internal.teamplayer.query.service.impl.TeamPlayerQueryTxService;
import com.dhoon.transfertracker.internal.teamplayer.repository.TeamPlayerRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.*;


@ExtendWith(MockitoExtension.class)
public class TeamPlayerQueryTxServiceTest {

    @Mock
    private TeamPlayerRepository teamPlayerRepository;

    @Mock
    private TeamRepository teamRepository;

    @InjectMocks
    private TeamPlayerQueryTxService teamPlayerQueryTxService;

    @Test
    @DisplayName("팀에 소속된 선수 목록을 정상적으로 조회한다.")
    void getTeamPlayers_success() {
        // given
        Team team = Team.of("testTeam", 111L);
        ReflectionTestUtils.setField(team, "id", 1L);

        given(teamRepository.findById(1L))
                .willReturn(Optional.of(team));

        Player player1 = Player.of("player-1", 222L);
        Player player2 = Player.of("player-2", 333L);
        Player player3 = Player.of("player-3", 444L);

        TeamPlayer tp1 = TeamPlayer.of(team, player1);
        TeamPlayer tp2 = TeamPlayer.of(team, player2);
        TeamPlayer tp3 = TeamPlayer.of(team, player3);

        given(teamPlayerRepository.findAllByTeamIdWithLazyEntity(1L))
                .willReturn(List.of(tp1, tp2, tp3));

        // when
        TeamPlayersResponseDto response = teamPlayerQueryTxService.getTeamPlayers(1L);

        // then
        assertThat(response.getTeamPlayers()).extracting(TeamPlayerItemResponseDto::getPlayerName)
                .containsExactlyInAnyOrder("player-1", "player-2", "player-3");

        verify(teamPlayerRepository, times(1)).findAllByTeamIdWithLazyEntity(1L);
        verify(teamRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("팀에 소속된 선수가 존재하지 않는경우 빈 리스트를 반환한다.")
    void getTeamPlayers_empty() {
        // given
        Team team = Team.of("testTeam", 111L);
        ReflectionTestUtils.setField(team, "id", 1L);

        given(teamRepository.findById(1L))
                .willReturn(Optional.of(team));

        given(teamPlayerRepository.findAllByTeamIdWithLazyEntity(1L))
                .willReturn(List.of());

        // when
        TeamPlayersResponseDto response = teamPlayerQueryTxService.getTeamPlayers(1L);

        // then
        assertThat(response.getTeamPlayers()).isEmpty();

        verify(teamPlayerRepository, times(1)).findAllByTeamIdWithLazyEntity(1L);
        verify(teamRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("팀이 존재하지 않는경우 NotFoundTeamException 이 발생한다.")
    void getTeamPlayers_notFoundTeam() {
        // given
        given(teamRepository.findById(1L))
                .willReturn(Optional.empty());

        // when && then
        assertThatThrownBy(() -> teamPlayerQueryTxService.getTeamPlayers(1L))
                .isInstanceOf(NotFoundTeamException.class);

        verify(teamRepository, times(1)).findById(1L);
        verifyNoInteractions(teamPlayerRepository);
    }




}
