package com.dhoon.transfertracker.internal.player.query.service;

import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.exception.InvalidPlayerIdException;
import com.dhoon.transfertracker.internal.player.exception.InvalidPlayerSearchPageValueException;
import com.dhoon.transfertracker.internal.player.query.dto.response.PlayerItemResponseDto;
import com.dhoon.transfertracker.internal.player.query.dto.response.PlayersResponseDto;
import com.dhoon.transfertracker.internal.player.query.service.impl.PlayerQueryOrchestrationService;
import com.dhoon.transfertracker.internal.player.query.service.impl.PlayerQueryTxService;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.teamplayer.domain.TeamPlayer;
import com.dhoon.transfertracker.internal.teamplayer.repository.TeamPlayerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.mockito.BDDMockito.*;
import static org.assertj.core.api.Assertions.*;


@ExtendWith(MockitoExtension.class)
public class PlayerQueryOrchestrationServiceTest {

    @Mock
    private PlayerQueryTxService playerQueryTxService;

    @InjectMocks
    private PlayerQueryOrchestrationService playerQueryOrchestrationService;

    @Test
    @DisplayName("정상적인 선수 ID로 조회하면 선수 정보를 반환한다. [ FA ]")
    void getPlayer_success_FreeAgent() {
        // given
        Player player = Player.of("testPlayer", 111L);
        ReflectionTestUtils.setField(player, "id", 1L);

        PlayerItemResponseDto expected = PlayerItemResponseDto.of(player);

        given(playerQueryTxService.getPlayer(1L))
                .willReturn(expected);

        // when
        PlayerItemResponseDto response = playerQueryOrchestrationService.getPlayer(1L);

        // then
        assertThat(response).isSameAs(expected);

        assertThat(response.getPlayerName()).isEqualTo("testPlayer");
        assertThat(response.getPlayerId()).isEqualTo(1L);
        assertThat(response.getTeamName()).isNull();

        verify(playerQueryTxService, times(1)).getPlayer(1L);
    }

    @Test
    @DisplayName("정상적인 선수 ID로 조회하면 선수 정보를 반환한다. [ 팀에 속한 선수 ]")
    void getPlayer_success_teamPlayer() {
        // given
        Team team = Team.of("testTeam", 999L);

        Player player = Player.of("testPlayer", 111L);
        ReflectionTestUtils.setField(player, "id", 1L);

        TeamPlayer teamPlayer = TeamPlayer.of(team, player);

        PlayerItemResponseDto expected = PlayerItemResponseDto.of(teamPlayer);

        given(playerQueryTxService.getPlayer(1L))
                .willReturn(expected);

        // when
        PlayerItemResponseDto response = playerQueryOrchestrationService.getPlayer(1L);

        // then
        assertThat(response).isSameAs(expected);

        assertThat(response.getPlayerId()).isEqualTo(1L);
        assertThat(response.getPlayerName()).isEqualTo("testPlayer");
        assertThat(response.getTeamName()).isEqualTo("testTeam");


        verify(playerQueryTxService, times(1)).getPlayer(1L);
    }

    @Test
    @DisplayName("선수 ID가 null이면 InvalidPlayerIdException이 발생한다.")
    void getPlayer_invalidId() {
        // when & then
        assertThatThrownBy(() -> playerQueryOrchestrationService.getPlayer(null))
                .isInstanceOf(InvalidPlayerIdException.class);

        verifyNoInteractions(playerQueryTxService);
    }


    @Test
    @DisplayName("선수 목록 조회 시 검색 조건과 페이징 설정을 전달한다.")
    void getPlayers_success() {
        // given
        Team team = Team.of("testTeam", 999L);
        team.assignTeamLeague(LeagueCode.LA_LIGA);
        ReflectionTestUtils.setField(team, "id", 10L);

        Player player = Player.of("Messi", 111L);
        ReflectionTestUtils.setField(player, "id", 1L);

        TeamPlayer teamPlayer = TeamPlayer.of(team, player);

        PlayersResponseDto expected = PlayersResponseDto.of(
                List.of(PlayerItemResponseDto.of(teamPlayer)),
                true,
                false
        );

        given(playerQueryTxService.getPlayers(
                any(Pageable.class),
                eq("Messi"),
                eq(LeagueCode.LA_LIGA),
                eq(10L)
        )).willReturn(expected);

        // when
        PlayersResponseDto response =
                playerQueryOrchestrationService.getPlayers(
                        1,
                        "    Messi    ",
                        LeagueCode.LA_LIGA,
                        10L
                );

        // then
        ArgumentCaptor<Pageable> captor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(playerQueryTxService, times(1))
                .getPlayers(
                        captor.capture(),
                        eq("Messi"),
                        eq(LeagueCode.LA_LIGA),
                        eq(10L)
                );

        Pageable captured = captor.getValue();

        // 페이지 번호 및 크기 검증
        assertThat(captured.getPageNumber()).isEqualTo(1);
        assertThat(captured.getPageSize()).isEqualTo(50);

        // 정렬 조건 및 우선순위 검증
        assertThat(captured.getSort().toList())
                .containsExactly(
                        Sort.Order.asc("player.playerName"),
                        Sort.Order.asc("player.id")
                );

        // TxService가 반환한 DTO를 그대로 전달하는지 검증
        assertThat(response).isSameAs(expected);
    }

    @Test
    @DisplayName("페이지 번호가 음수이면 InvalidPlayerSearchPageValueException이 발생한다.")
    void getPlayers_invalidPage() {
        // when & then
        assertThatThrownBy(() -> playerQueryOrchestrationService.getPlayers(
                -2, "      Messi  ", LeagueCode.EPL, 10L))
                .isInstanceOf(InvalidPlayerSearchPageValueException.class);

        verifyNoInteractions(playerQueryTxService);
    }

    @Test
    @DisplayName("검색어가 null이면 빈 문자열로 변환하여 전달한다.") // null ->  ""
    void getPlayers_nullKeyword() {

        // when
        playerQueryOrchestrationService.getPlayers(
                0, null, null, null
        );

        // then
        verify(playerQueryTxService, times(1)).getPlayers(
                any(Pageable.class),
                eq(""),
                isNull(),
                isNull()
        );

    }


}
