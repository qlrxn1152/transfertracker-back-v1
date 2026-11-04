package com.dhoon.transfertracker.internal.teamplayer.query.service.impl;

import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.exception.InvalidTeamIdException;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.teamplayer.domain.TeamPlayer;
import com.dhoon.transfertracker.internal.teamplayer.query.dto.response.TeamPlayerItemResponseDto;
import com.dhoon.transfertracker.internal.teamplayer.query.dto.response.TeamPlayersResponseDto;
import com.dhoon.transfertracker.internal.teamplayer.query.service.TeamPlayerService;
import com.dhoon.transfertracker.internal.teamplayer.query.service.impl.TeamPlayerQueryOrchestrationService;
import com.dhoon.transfertracker.internal.teamplayer.query.service.impl.TeamPlayerQueryTxService;
import com.dhoon.transfertracker.internal.teamplayer.repository.TeamPlayerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class TeamPlayerQueryOrchestrationServiceTest {

    @Mock
    private TeamPlayerService txService;

    @InjectMocks
    private TeamPlayerQueryOrchestrationService orchService;

    @Test
    @DisplayName("정상적인 팀 ID를 전달하면 소속 선수 목록을 반환한다.")
    void getTeamPlayers_success() {
        // given
        Team team = Team.of("testTeam", 111L);
        ReflectionTestUtils.setField(team, "id", 1L);

        Player p1 = Player.of("player-1", 222L);
        Player p2 = Player.of("player-2", 333L);
        Player p3 = Player.of("player-3", 444L);

        TeamPlayer tp1 = TeamPlayer.of(team, p1);
        TeamPlayer tp2 = TeamPlayer.of(team, p2);
        TeamPlayer tp3 = TeamPlayer.of(team, p3);

        TeamPlayersResponseDto expected = TeamPlayersResponseDto.of(
                List.of(
                        TeamPlayerItemResponseDto.of(tp1),
                        TeamPlayerItemResponseDto.of(tp2),
                        TeamPlayerItemResponseDto.of(tp3)
                )
        );

        given(txService.getTeamPlayers(1L))
                .willReturn(expected);

        // when
        TeamPlayersResponseDto response = orchService.getTeamPlayers(1L);

        // then
        assertThat(response).isSameAs(expected);
        assertThat(response.getTeamPlayers()).extracting(TeamPlayerItemResponseDto::getPlayerName)
                        .containsExactlyInAnyOrder("player-1", "player-2", "player-3");

        verify(txService, times(1)).getTeamPlayers(1L);
    }

    @Test
    @DisplayName("팀 ID가 null이면 InvalidTeamIdException이 발생한다.")
    void getTeamPlayers_invalidTeamId() {
        // when & then
        assertThatThrownBy(() -> orchService.getTeamPlayers(null))
                .isInstanceOf(InvalidTeamIdException.class);

        verifyNoInteractions(txService);
    }


}
