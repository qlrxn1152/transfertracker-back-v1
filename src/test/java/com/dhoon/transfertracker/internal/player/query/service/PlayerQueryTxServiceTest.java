package com.dhoon.transfertracker.internal.player.query.service;

import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.exception.NotFoundPlayerException;
import com.dhoon.transfertracker.internal.player.query.dto.response.PlayerItemResponseDto;
import com.dhoon.transfertracker.internal.player.query.dto.response.PlayersResponseDto;
import com.dhoon.transfertracker.internal.player.query.service.impl.PlayerQueryTxService;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.teamplayer.domain.TeamPlayer;
import com.dhoon.transfertracker.internal.teamplayer.repository.TeamPlayerRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;


@ExtendWith(MockitoExtension.class)
public class PlayerQueryTxServiceTest {

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private TeamPlayerRepository teamPlayerRepository;
    
    @InjectMocks
    private PlayerQueryTxService playerQueryTxService;

    @Test
    @DisplayName(value = "팀에 소속된 선수를 조회하면 소속팀 정보를 함께 반환한다.")
    void getPlayer_success_teamPlayer() throws Exception {
        // given
        Player player = Player.of("testPlayer", 123L);
        ReflectionTestUtils.setField(player, "id", 1L);

        Team team = Team.of("testTeam", 444L);
        TeamPlayer teamPlayer = TeamPlayer.of(team, player);

        given(playerRepository.findById(1L))
                .willReturn(Optional.of(player));

        given(teamPlayerRepository.findByPlayerId(1L))
                .willReturn(Optional.of(teamPlayer));

        // when
        PlayerItemResponseDto response = playerQueryTxService.getPlayer(1L);

        // then
        assertThat(response.getPlayerId()).isEqualTo(1L);
        assertThat(response.getPlayerName()).isEqualTo("testPlayer");
        assertThat(response.getTeamName()).isEqualTo("testTeam");

        verify(playerRepository, times(1)).findById(1L);
        verify(teamPlayerRepository, times(1)).findByPlayerId(1L);
    }

    @Test
    @DisplayName(value = "FA 선수를 조회하면 소속팀 정보는 반환되지않고, 선수에 대한 정보만 반환한다.")
    void getPlayer_success_freeAgent() throws Exception {
        // given
        Player player = Player.of("testPlayer", 123L);
        ReflectionTestUtils.setField(player, "id", 1L);

        given(playerRepository.findById(1L))
                .willReturn(Optional.of(player));

        given(teamPlayerRepository.findByPlayerId(1L))
                .willReturn(Optional.empty());

        // when
        PlayerItemResponseDto response = playerQueryTxService.getPlayer(1L);

        // then
        assertThat(response.getPlayerId()).isEqualTo(1L);
        assertThat(response.getPlayerName()).isEqualTo("testPlayer");

        assertThat(response.getTeamName()).isNull();

        verify(playerRepository, times(1)).findById(1L);
        verify(teamPlayerRepository, times(1)).findByPlayerId(1L);
    }

    @Test
    @DisplayName("존재하지 않는 선수를 조회하면 예외가 발생한다.")
    void getPlayer_notFound() {
        // given
        given(playerRepository.findById(1L))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> playerQueryTxService.getPlayer(1L))
                .isInstanceOf(NotFoundPlayerException.class);

        verify(playerRepository, times(1)).findById(1L);
        verifyNoInteractions(teamPlayerRepository);
    }

    @Test
    @DisplayName("선수 목록 조회 시 선수 정보와 페이지 이동 정보를 반환한다.")
    void getPlayers_success() {
        // given
        Team team = Team.of("testTeam", 999L);

        Player p1 = Player.of("testPlayer-1", 111L);
        Player p2 = Player.of("testPlayer-2", 222L);

        TeamPlayer tp1 = TeamPlayer.of(team, p1);
        TeamPlayer tp2 = TeamPlayer.of(team, p2);

        Pageable pageable = PageRequest.of(0, 2); // 1페이지당 2개의 선수데이터.

        Slice<TeamPlayer> slice = new SliceImpl<>(
                List.of(tp1, tp2),
                pageable,
                true
        );

        given(teamPlayerRepository.findTeamPlayers("", pageable, null, null)) // 검색어 ( 필터 ) 없이 조회
                .willReturn(slice);

        // when
        PlayersResponseDto response = playerQueryTxService.getPlayers(pageable, "", null, null);

        // then
        assertThat(response.getPlayers()).hasSize(2);
        assertThat(response.getPlayers())
                .extracting(PlayerItemResponseDto::getPlayerName)
                .containsExactlyInAnyOrder("testPlayer-1", "testPlayer-2");

        assertThat(response.isHasNext()).isTrue();
        assertThat(response.isHasPrevious()).isFalse();

        verify(teamPlayerRepository, times(1)).findTeamPlayers("", pageable, null, null);
    }

    @Test
    @DisplayName("조회된 선수가 없으면 빈 목록과 페이지 이동 정보를 반환한다.")
    void getPlayers_empty() {
        // given
        Pageable pageable = PageRequest.of(0, 2);

        Slice<TeamPlayer> slice = new SliceImpl<>(
                List.of(),
                pageable,
                false
        );

        given(teamPlayerRepository.findTeamPlayers("", pageable, null, null))
                .willReturn(slice);

        // when
        PlayersResponseDto response = playerQueryTxService.getPlayers(pageable, "", null, null);

        // then
        assertThat(response.getPlayers()).isEmpty();
        assertThat(response.isHasNext()).isFalse();
        assertThat(response.isHasPrevious()).isFalse();

        verify(teamPlayerRepository, times(1)).findTeamPlayers("", pageable, null, null);
    }

    @Test
    @DisplayName("마지막 페이지 조회 시 이전 페이지는 존재하고 다음 페이지는 존재하지 않는다.")
    void getPlayers_lastPage() {
        // given
        Team team = Team.of("testTeam", 999L);

        Player p1 = Player.of("testPlayer-1", 111L);

        TeamPlayer tp1 = TeamPlayer.of(team, p1);

        Pageable pageable = PageRequest.of(1, 2); // 1페이지당 2개의 선수데이터.

        Slice<TeamPlayer> slice = new SliceImpl<>(
                List.of(tp1),
                pageable,
                false
        );

        given(teamPlayerRepository.findTeamPlayers("", pageable, null, null)) // 검색어 ( 필터 ) 없이 조회
                .willReturn(slice);

        // when
        PlayersResponseDto response = playerQueryTxService.getPlayers(pageable, "", null, null);

        // then
        assertThat(response.getPlayers()).hasSize(1);
        assertThat(response.getPlayers().getFirst().getPlayerName()).isEqualTo("testPlayer-1");

        assertThat(response.isHasNext()).isFalse();
        assertThat(response.isHasPrevious()).isTrue();

        verify(teamPlayerRepository, times(1)).findTeamPlayers("", pageable, null, null);
    }

}
