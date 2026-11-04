package com.dhoon.transfertracker.internal.teamplayer.sync.service.impl;

import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.teamplayer.domain.TeamPlayer;
import com.dhoon.transfertracker.internal.teamplayer.repository.TeamPlayerRepository;
import com.dhoon.transfertracker.internal.teamplayer.sync.dto.response.TeamPlayerSaveResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class TeamPlayerSyncTxServiceTest {

    @Mock
    private TeamPlayerRepository teamPlayerRepository;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private TeamRepository teamRepository;

    @InjectMocks
    private TeamPlayerSyncTxService teamPlayerSyncTxService;

    JsonMapper jsonMapper = new JsonMapper();


    @Test
    @DisplayName("선수, 팀, 소속 관계가 이미 존재하면 기존 데이터를 반환하고 저장하지 않는다.")
    void getOrCreateTeamPlayerResponse_success_existing() throws Exception {
        // given
        JsonNode playerData = jsonMapper.readTree("""
                {
                    "id":111,
                    "name":"Messi"
                }
                
                """);

        JsonNode teamData = jsonMapper.readTree("""
                {
                    "id":222,
                    "name":"Inter Miami"
                }
                
                """);

        Player player = Player.of("Messi", 111L);
        ReflectionTestUtils.setField(player, "id", 1L);

        Team team = Team.of("Inter Miami", 222L);
        ReflectionTestUtils.setField(team, "id", 2L);

        TeamPlayer teamPlayer = TeamPlayer.of(team, player);

        given(playerRepository.findByApiFootballId(111L))
                .willReturn(Optional.of(player));

        given(teamRepository.findByApiFootballId(222L))
                .willReturn(Optional.of(team));

        given(teamPlayerRepository.findByPlayerIdAndTeamId(1L, 2L))
                .willReturn(Optional.of(teamPlayer));

        // when
        TeamPlayerSaveResponseDto response = teamPlayerSyncTxService.getOrCreateTeamPlayerResponse(playerData, teamData);

        // then
        assertThat(response.getPlayerId()).isEqualTo(1L);
        assertThat(response.getTeamId()).isEqualTo(2L);

        verify(playerRepository, times(1)).findByApiFootballId(111L);
        verify(teamRepository, times(1)).findByApiFootballId(222L);
        verify(teamPlayerRepository, times(1)).findByPlayerIdAndTeamId(1L, 2L);

        verify(playerRepository, never()).save(any(Player.class));
        verify(teamRepository, never()).save(any(Team.class));
        verify(teamPlayerRepository, never()).save(any(TeamPlayer.class));
    }

    @Test
    @DisplayName("팀은 존재하고, 선수가 존재하지 않으면 선수와 소속 관계를 새로 생성한다.")
    void getOrCreateTeamPlayerResponse_success_newPlayer() {
        // given
        JsonNode playerData = jsonMapper.readTree("""
                {
                    "id":111,
                    "name":"Messi"
                }
                
                """);

        JsonNode teamData = jsonMapper.readTree("""
                {
                    "id":222,
                    "name":"Inter Miami"
                }
                
                """);

        Team team = Team.of("Inter Miami", 222L);
        ReflectionTestUtils.setField(team, "id", 2L);

        given(teamRepository.findByApiFootballId(222L))
                .willReturn(Optional.of(team));

        given(playerRepository.findByApiFootballId(111L))
                .willReturn(Optional.empty());

        given(playerRepository.save(any(Player.class)))
                .willAnswer(invocation -> {
                    Player savedPlayer = invocation.getArgument(0);
                    ReflectionTestUtils.setField(savedPlayer, "id", 1L);
                    return savedPlayer;
                });

        given(teamPlayerRepository.findByPlayerIdAndTeamId(1L, 2L))
                .willReturn(Optional.empty());

        given(teamPlayerRepository.save(any(TeamPlayer.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        TeamPlayerSaveResponseDto response = teamPlayerSyncTxService.getOrCreateTeamPlayerResponse(playerData, teamData);

        // then
        ArgumentCaptor<Player> playerCaptor = ArgumentCaptor.forClass(Player.class);
        ArgumentCaptor<TeamPlayer> teamPlayerCaptor = ArgumentCaptor.forClass(TeamPlayer.class);

        verify(playerRepository, times(1)).findByApiFootballId(111L);
        verify(playerRepository, times(1)).save(playerCaptor.capture());

        verify(teamRepository, times(1)).findByApiFootballId(222L);
        verify(teamRepository, never()).save(any(Team.class));

        verify(teamPlayerRepository, times(1)).save(teamPlayerCaptor.capture());
        verify(teamPlayerRepository, times(1)).findByPlayerIdAndTeamId(1L, 2L);

        Player savedPlayer = playerCaptor.getValue();
        TeamPlayer savedTeamPlayer = teamPlayerCaptor.getValue();

        assertThat(savedPlayer.getId()).isEqualTo(1L);
        assertThat(savedPlayer.getDisplayName()).isEqualTo("Messi");
        assertThat(savedPlayer.getApiFootballId()).isEqualTo(111L);

        assertThat(savedTeamPlayer.getPlayer()).isSameAs(savedPlayer);
        assertThat(savedTeamPlayer.getTeam()).isSameAs(team);

        assertThat(response.getPlayerId()).isEqualTo(1L);
        assertThat(response.getTeamId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("선수는 존재하고 팀이 존재하지 않으면 팀과 소속 관계를 새로 생성한다.")
    void getOrCreateTeamPlayerResponse_success_newTeam() {
        // given
        JsonNode playerData = jsonMapper.readTree("""
                {
                    "id":111,
                    "name":"Messi"
                }
                
                """);

        JsonNode teamData = jsonMapper.readTree("""
                {
                    "id":222,
                    "name":"Inter Miami"
                }
                
                """);

        Player player = Player.of("Messi", 111L);
        ReflectionTestUtils.setField(player, "id", 1L);

        given(playerRepository.findByApiFootballId(111L))
                .willReturn(Optional.of(player));

        given(teamRepository.findByApiFootballId(222L))
                .willReturn(Optional.empty());

        given(teamRepository.save(any(Team.class)))
                .willAnswer(invocation -> {
                    Team savedTeam = invocation.getArgument(0);
                    ReflectionTestUtils.setField(savedTeam, "id", 2L);
                    return savedTeam;
                });

        given(teamPlayerRepository.findByPlayerIdAndTeamId(1L, 2L))
                .willReturn(Optional.empty());

        given(teamPlayerRepository.save(any(TeamPlayer.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        TeamPlayerSaveResponseDto response = teamPlayerSyncTxService.getOrCreateTeamPlayerResponse(playerData, teamData);

        // then
        ArgumentCaptor<TeamPlayer> teamPlayerCaptor = ArgumentCaptor.forClass(TeamPlayer.class);
        ArgumentCaptor<Team> teamCaptor = ArgumentCaptor.forClass(Team.class);

        verify(playerRepository, times(1)).findByApiFootballId(111L);
        verify(playerRepository, never()).save(any(Player.class));

        verify(teamRepository, times(1)).findByApiFootballId(222L);
        verify(teamRepository, times(1)).save(teamCaptor.capture());

        verify(teamPlayerRepository, times(1)).findByPlayerIdAndTeamId(1L, 2L);
        verify(teamPlayerRepository, times(1)).save(teamPlayerCaptor.capture());

        Team savedTeam = teamCaptor.getValue();
        TeamPlayer savedTeamPlayer = teamPlayerCaptor.getValue();

        assertThat(savedTeam.getId()).isEqualTo(2L);
        assertThat(savedTeam.getDisplayName()).isEqualTo("Inter Miami");
        assertThat(savedTeam.getApiFootballId()).isEqualTo(222L);

        assertThat(savedTeamPlayer.getPlayer()).isSameAs(player);
        assertThat(savedTeamPlayer.getTeam()).isSameAs(savedTeam);

        assertThat(response.getPlayerId()).isEqualTo(1L);
        assertThat(response.getTeamId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("선수와 팀이 모두 없으면 선수, 팀, 소속 관계를 새로 생성한다.")
    void getOrCreateTeamPlayerResponse_success_newAll() {
        // given
        JsonNode playerData = jsonMapper.readTree("""
                {
                    "id":111,
                    "name":"Messi"
                }
                
                """);

        JsonNode teamData = jsonMapper.readTree("""
                {
                    "id":222,
                    "name":"Inter Miami"
                }
                
                """);

        given(playerRepository.findByApiFootballId(111L))
                .willReturn(Optional.empty());

        given(playerRepository.save(any(Player.class)))
                .willAnswer(invocation -> {
                    Player savedPlayer = invocation.getArgument(0);
                    ReflectionTestUtils.setField(savedPlayer, "id", 1L);
                    return savedPlayer;
                });

        given(teamRepository.findByApiFootballId(222L))
                .willReturn(Optional.empty());

        given(teamRepository.save(any(Team.class)))
                .willAnswer(invocation -> {
                    Team savedTeam = invocation.getArgument(0);
                    ReflectionTestUtils.setField(savedTeam, "id", 2L);
                    return savedTeam;
                });

        given(teamPlayerRepository.findByPlayerIdAndTeamId(1L, 2L))
                .willReturn(Optional.empty());

        given(teamPlayerRepository.save(any(TeamPlayer.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        TeamPlayerSaveResponseDto response = teamPlayerSyncTxService.getOrCreateTeamPlayerResponse(playerData, teamData);

        // then
        ArgumentCaptor<TeamPlayer> teamPlayerCaptor = ArgumentCaptor.forClass(TeamPlayer.class);
        ArgumentCaptor<Team> teamCaptor = ArgumentCaptor.forClass(Team.class);
        ArgumentCaptor<Player> playerCaptor = ArgumentCaptor.forClass(Player.class);

        verify(playerRepository, times(1)).findByApiFootballId(111L);
        verify(playerRepository, times(1)).save(playerCaptor.capture());
        Player savedPlayer = playerCaptor.getValue();

        verify(teamRepository, times(1)).findByApiFootballId(222L);
        verify(teamRepository, times(1)).save(teamCaptor.capture());
        Team savedTeam = teamCaptor.getValue();

        verify(teamPlayerRepository, times(1)).findByPlayerIdAndTeamId(1L, 2L);
        verify(teamPlayerRepository, times(1)).save(teamPlayerCaptor.capture());
        TeamPlayer savedTeamPlayer = teamPlayerCaptor.getValue();

        assertThat(savedPlayer.getId()).isEqualTo(1L);
        assertThat(savedPlayer.getDisplayName()).isEqualTo("Messi");
        assertThat(savedPlayer.getApiFootballId()).isEqualTo(111L);

        assertThat(savedTeam.getId()).isEqualTo(2L);
        assertThat(savedTeam.getDisplayName()).isEqualTo("Inter Miami");
        assertThat(savedTeam.getApiFootballId()).isEqualTo(222L);

        assertThat(savedTeamPlayer.getPlayer()).isSameAs(savedPlayer);
        assertThat(savedTeamPlayer.getTeam()).isSameAs(savedTeam);

        assertThat(response.getPlayerId()).isEqualTo(1L);
        assertThat(response.getTeamId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("선수와 팀은 존재하지만 소속 관계가 없으면 소속 관계만 생성한다.")
    void getOrCreateTeamPlayerResponse_success_newRelation() {
        // given
        JsonNode playerData = jsonMapper.readTree("""
                {
                    "id":111,
                    "name":"Messi"
                }
                
                """);

        JsonNode teamData = jsonMapper.readTree("""
                {
                    "id":222,
                    "name":"Inter Miami"
                }
                
                """);

        Player player = Player.of("Messi", 111L);
        ReflectionTestUtils.setField(player, "id", 1L);
        given(playerRepository.findByApiFootballId(111L))
                .willReturn(Optional.of(player));

        Team team = Team.of("Inter Miami", 222L);
        ReflectionTestUtils.setField(team, "id", 2L);
        given(teamRepository.findByApiFootballId(222L))
                .willReturn(Optional.of(team));

        given(teamPlayerRepository.findByPlayerIdAndTeamId(1L, 2L))
                .willReturn(Optional.empty());

        given(teamPlayerRepository.save(any(TeamPlayer.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        TeamPlayerSaveResponseDto response = teamPlayerSyncTxService.getOrCreateTeamPlayerResponse(playerData, teamData);

        // then
        ArgumentCaptor<TeamPlayer> teamPlayerCaptor = ArgumentCaptor.forClass(TeamPlayer.class);

        verify(playerRepository, times(1)).findByApiFootballId(111L);
        verify(playerRepository, never()).save(any(Player.class));

        verify(teamRepository, times(1)).findByApiFootballId(222L);
        verify(teamRepository, never()).save(any(Team.class));

        verify(teamPlayerRepository, times(1)).findByPlayerIdAndTeamId(1L, 2L);
        verify(teamPlayerRepository, times(1)).save(teamPlayerCaptor.capture());

        TeamPlayer savedTeamPlayer = teamPlayerCaptor.getValue();

        assertThat(response.getPlayerId()).isEqualTo(1L);
        assertThat(response.getTeamId()).isEqualTo(2L);

        assertThat(savedTeamPlayer.getPlayer()).isSameAs(player);
        assertThat(savedTeamPlayer.getTeam()).isSameAs(team);
    }
}
