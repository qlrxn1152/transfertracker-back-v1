package com.dhoon.transfertracker.internal.transfer.sync.service.impl;

import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.exception.NotFoundTeamException;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.transfer.domain.Transfer;
import com.dhoon.transfertracker.internal.transfer.query.dto.response.PlayerTransfersResponseDto;
import com.dhoon.transfertracker.internal.transfer.repository.TransferRepository;
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

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class TransferSyncTxServiceTest {

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private TeamRepository teamRepository;

    @InjectMocks
    private TransferSyncTxService txService;

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    @DisplayName("기존 선수가 존재하고 이적 기록이 없으면 저장하지 않는다.")
    void getOrCreatePlayerTransfersResponse_existingPlayer_emptyTransfers() {

        // given
        Player player = Player.of("Messi", 111L);
        ReflectionTestUtils.setField(player, "id", 1L);

        JsonNode playerData = jsonMapper.readTree("""
                {
                    "id": 111,
                    "name": "Messi"
                }
                """);

        JsonNode transferDatas = jsonMapper.readTree("[]");

        given(playerRepository.findByApiFootballId(111L))
                .willReturn(Optional.of(player));

        // when
        PlayerTransfersResponseDto response = txService.getOrCreatePlayerTransfersResponse(playerData, transferDatas);

        // then
        assertThat(response.getPlayerName()).isEqualTo("Messi");
        assertThat(response.getPlayerTransfers()).isEmpty();

        verify(playerRepository, times(1)).findByApiFootballId(111L);
        verify(playerRepository, never()).save(any(Player.class));

        verifyNoInteractions(teamRepository);
        verifyNoInteractions(transferRepository);
    }

    @Test
    @DisplayName("선수가 존재하지 않으면 새 선수를 저장한다.")
    void getOrCreatePlayerTransfersResponse_newPlayer_emptyTransfers() {

        // given
        JsonNode playerData = jsonMapper.readTree("""
            {
                "id": 111,
                "name": "Messi"
            }
            """);

        JsonNode transferDatas = jsonMapper.readTree("[]");

        given(playerRepository.findByApiFootballId(111L))
                .willReturn(Optional.empty());

        given(playerRepository.save(any(Player.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        PlayerTransfersResponseDto response = txService.getOrCreatePlayerTransfersResponse(playerData, transferDatas);

        // then
        ArgumentCaptor<Player> captor = ArgumentCaptor.forClass(Player.class);

        assertThat(response.getPlayerName()).isEqualTo("Messi");
        assertThat(response.getPlayerTransfers()).isEmpty();

        verify(playerRepository, times(1)).findByApiFootballId(111L);
        verify(playerRepository, times(1)).save(captor.capture());

        Player savedPlayer = captor.getValue();

        assertThat(savedPlayer.getDisplayName()).isEqualTo("Messi");
        assertThat(savedPlayer.getApiFootballId()).isEqualTo(111L);

        verifyNoInteractions(teamRepository);
        verifyNoInteractions(transferRepository);
    }

    @Test
    @DisplayName("선수와 팀, 이적 기록이 모두 존재하면 중복 저장하지 않는다.")
    void getOrCreatePlayerTransfersResponse_existingTransfer() {
        // given
        JsonNode node = getExistingTransferNode();
        JsonNode playerData = node.get("response").get(0).get("player");
        JsonNode transferDatas = node.get("response").get(0).get("transfers");

        Player messi = Player.of("Messi", 111L);
        ReflectionTestUtils.setField(messi, "id", 1L);

        given(playerRepository.findByApiFootballId(111L))
                .willReturn(Optional.of(messi));

        Team barcelona = Team.of("Barcelona", 44L);
        ReflectionTestUtils.setField(barcelona, "id", 4L);

        Team psg = Team.of("PSG", 33L);
        ReflectionTestUtils.setField(psg, "id", 3L);

        given(teamRepository.findByApiFootballId(44L))
                .willReturn(Optional.of(barcelona));
        given(teamRepository.findByApiFootballId(33L))
                .willReturn(Optional.of(psg));

        Transfer transfer = Transfer.of(messi, barcelona, psg, "Free", LocalDate.parse("2026-07-01"));

        given(transferRepository.findByInTeamIdAndOutTeamIdAndPlayerIdAndTransferDate(4L, 3L, 1L, LocalDate.parse("2026-07-01")))
                .willReturn(Optional.of(transfer));


        // when
        PlayerTransfersResponseDto response = txService.getOrCreatePlayerTransfersResponse(playerData, transferDatas);

        // then
        verify(playerRepository, times(1)).findByApiFootballId(111L);
        verify(playerRepository, never()).save(any(Player.class));

        verify(teamRepository, times(1)).findByApiFootballId(44L);
        verify(teamRepository, times(1)).findByApiFootballId(33L);
        verify(teamRepository, never()).save(any(Team.class));

        verify(transferRepository, times(1)).findByInTeamIdAndOutTeamIdAndPlayerIdAndTransferDate(4L, 3L, 1L, LocalDate.parse("2026-07-01"));
        verify(transferRepository, never()).save(any(Transfer.class));

        assertThat(response.getPlayerName()).isEqualTo("Messi");
        assertThat(response.getPlayerTransfers()).hasSize(1);

        assertThat(response.getPlayerTransfers().get(0).getDate()).isEqualTo(LocalDate.parse("2026-07-01"));
        assertThat(response.getPlayerTransfers().get(0).getType()).isEqualTo("Free");
        assertThat(response.getPlayerTransfers().get(0).getInTeamName()).isEqualTo("Barcelona");
        assertThat(response.getPlayerTransfers().get(0).getOutTeamName()).isEqualTo("PSG");
    }

    @Test
    @DisplayName("기존 선수와 팀은 존재하지만 이적 기록이 없으면 새로 저장한다.")
    void getOrCreatePlayerTransfersResponse_newTransfer() {
        // given
        JsonNode node = getExistingTransferNode();

        JsonNode playerData =
                node.get("response").get(0).get("player");

        JsonNode transferDatas =
                node.get("response").get(0).get("transfers");

        Player messi = Player.of("Messi", 111L);
        ReflectionTestUtils.setField(messi, "id", 1L);

        given(playerRepository.findByApiFootballId(111L))
                .willReturn(Optional.of(messi));

        Team barcelona = Team.of("Barcelona", 44L);
        ReflectionTestUtils.setField(barcelona, "id", 4L);

        Team psg = Team.of("PSG", 33L);
        ReflectionTestUtils.setField(psg, "id", 3L);

        // PSG -> BARCELONA ( MESSI )

        given(teamRepository.findByApiFootballId(44L))
                .willReturn(Optional.of(barcelona));
        given(teamRepository.findByApiFootballId(33L))
                .willReturn(Optional.of(psg));

        LocalDate transferDate = LocalDate.parse("2026-07-01");
        given(transferRepository.findByInTeamIdAndOutTeamIdAndPlayerIdAndTransferDate(4L, 3L, 1L, transferDate))
                .willReturn(Optional.empty()); // 해당 이적정보가 존재하지 않음.

        given(transferRepository.save(any(Transfer.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        PlayerTransfersResponseDto response = txService.getOrCreatePlayerTransfersResponse(playerData, transferDatas);

        // then
        verify(playerRepository, times(1)).findByApiFootballId(111L);
        verify(playerRepository, never()).save(any(Player.class));

        verify(teamRepository, times(1)).findByApiFootballId(44L);
        verify(teamRepository, times(1)).findByApiFootballId(33L);
        verify(teamRepository, never()).save(any(Team.class));

        ArgumentCaptor<Transfer> captor = ArgumentCaptor.forClass(Transfer.class);

        verify(transferRepository, times(1)).findByInTeamIdAndOutTeamIdAndPlayerIdAndTransferDate(4L, 3L, 1L, transferDate);
        verify(transferRepository, times(1)).save(captor.capture());

        Transfer savedTransfer = captor.getValue();

        assertThat(savedTransfer.getPlayer()).isSameAs(messi);
        assertThat(savedTransfer.getInTeam()).isSameAs(barcelona);
        assertThat(savedTransfer.getOutTeam()).isSameAs(psg);
        assertThat(savedTransfer.getTransferType()).isEqualTo("Free");
        assertThat(savedTransfer.getTransferDate()).isEqualTo(transferDate);

        assertThat(response.getPlayerName()).isEqualTo("Messi");
        assertThat(response.getPlayerTransfers()).hasSize(1);
        assertThat(response.getPlayerTransfers().get(0).getInTeamName()).isEqualTo("Barcelona");
        assertThat(response.getPlayerTransfers().get(0).getOutTeamName()).isEqualTo("PSG");
        assertThat(response.getPlayerTransfers().get(0).getType()).isEqualTo("Free");
        assertThat(response.getPlayerTransfers().get(0).getDate()).isEqualTo(transferDate);
    }

    @Test
    @DisplayName("팀이 존재하지 않으면 새 팀을 저장하고 이적 기록을 생성한다.")
    void getOrCreatePlayerTransfersResponse_newTeam() {
        // given
        JsonNode node = getExistingTransferNode();

        JsonNode playerData =
                node.get("response").get(0).get("player");

        JsonNode transferDatas =
                node.get("response").get(0).get("transfers");

        Player messi = Player.of("Messi", 111L);
        ReflectionTestUtils.setField(messi, "id", 1L);

        Team psg = Team.of("PSG", 33L);
        ReflectionTestUtils.setField(psg, "id", 3L);

        given(playerRepository.findByApiFootballId(111L))
                .willReturn(Optional.of(messi));

        given(teamRepository.findByApiFootballId(44L)) // barcelona
                .willReturn(Optional.empty());

        given(teamRepository.findByApiFootballId(33L))
                .willReturn(Optional.of(psg));

        given(teamRepository.save(any(Team.class)))
                .willAnswer(invocation -> {
                    Team savedTeam = invocation.getArgument(0);
                    ReflectionTestUtils.setField(savedTeam, "id", 4L);
                    return savedTeam;
                });

        given(transferRepository.findByInTeamIdAndOutTeamIdAndPlayerIdAndTransferDate(4L, 3L, 1L, LocalDate.parse("2026-07-01")))
                .willReturn(Optional.empty());

        given(transferRepository.save(any(Transfer.class)))
                .willAnswer(invocation -> {
                    Transfer savedTransfer = invocation.getArgument(0);
                    ReflectionTestUtils.setField(savedTransfer, "id", 998L);
                    return savedTransfer;
                });

        // when
        PlayerTransfersResponseDto response = txService.getOrCreatePlayerTransfersResponse(playerData, transferDatas);

        // then
        verify(playerRepository, times(1)).findByApiFootballId(111L);
        verify(playerRepository, never()).save(any(Player.class));

        ArgumentCaptor<Team> teamCaptor = ArgumentCaptor.forClass(Team.class);
        verify(teamRepository, times(1)).findByApiFootballId(44L);
        verify(teamRepository, times(1)).findByApiFootballId(33L);
        verify(teamRepository, times(1)).save(teamCaptor.capture());

        Team savedTeam = teamCaptor.getValue();
        assertThat(savedTeam.getId()).isEqualTo(4L);
        assertThat(savedTeam.getDisplayName()).isEqualTo("Barcelona");
        assertThat(savedTeam.getApiFootballId()).isEqualTo(44L);

        ArgumentCaptor<Transfer> transferCaptor = ArgumentCaptor.forClass(Transfer.class);
        verify(transferRepository, times(1)).findByInTeamIdAndOutTeamIdAndPlayerIdAndTransferDate(4L, 3L, 1L, LocalDate.parse("2026-07-01"));
        verify(transferRepository, times(1)).save(transferCaptor.capture());

        Transfer savedTransfer = transferCaptor.getValue();
        assertThat(savedTransfer.getId()).isEqualTo(998L);
        assertThat(savedTransfer.getOutTeam()).isSameAs(psg);
        assertThat(savedTransfer.getInTeam()).isSameAs(savedTeam);
        assertThat(savedTransfer.getPlayer()).isSameAs(messi);
        assertThat(savedTransfer.getTransferType()).isEqualTo("Free");
        assertThat(savedTransfer.getTransferDate()).isEqualTo(LocalDate.parse("2026-07-01"));

        assertThat(response.getPlayerName()).isEqualTo("Messi");
        assertThat(response.getPlayerTransfers()).hasSize(1);
        assertThat(response.getPlayerTransfers().get(0).getType()).isEqualTo("Free");
        assertThat(response.getPlayerTransfers().get(0).getDate()).isEqualTo(LocalDate.parse("2026-07-01"));
        assertThat(response.getPlayerTransfers().get(0).getOutTeamName()).isEqualTo("PSG");
        assertThat(response.getPlayerTransfers().get(0).getInTeamName()).isEqualTo("Barcelona");
    }

    @Test
    @DisplayName("팀이 존재하고, 한국어 팀 이름이 존재할경우 한국어 팀 이름을 반환한다.")
    void getDisplayTeamName_success() {
        // given
        Team team = Team.of("Barcelona", 44L);
        team.assignKoTeamName("바르셀로나");

        given(teamRepository.findByApiFootballId(44L))
                .willReturn(Optional.of(team));

        // when
        String responseTeamName = txService.getDisplayTeamName(44L);

        // then
        assertThat(responseTeamName).isEqualTo("바르셀로나");
    }

    @Test
    @DisplayName("팀이 존재하지만, 한국어 팀 이름이 존재하지 않을경우 영어(원문) 팀 이름을 반환한다.")
    void getDisplayTeamName_success_notAssignKoTeamName() {
        // given
        Team team = Team.of("Barcelona", 44L);

        given(teamRepository.findByApiFootballId(44L))
                .willReturn(Optional.of(team));

        // when
        String responseTeamName = txService.getDisplayTeamName(44L);

        // then
        assertThat(responseTeamName).isEqualTo("Barcelona");
    }

    @Test
    @DisplayName("팀이 존재하지 않으면 예외가 발생한다.")
    void getDisplayTeamName_notFound() {
        // given
        Long teamId = 441L;

        given(teamRepository.findByApiFootballId(teamId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> txService.getDisplayTeamName(teamId))
                .isInstanceOf(NotFoundTeamException.class);
        verify(teamRepository, times(1)).findByApiFootballId(teamId);
    }


    private JsonNode getExistingTransferNode() {
        return jsonMapper.readTree("""
        {
          "response": [
            {
              "player": {
                "id": 111,
                "name": "Messi"
              },
              "transfers": [
                {
                  "date": "2026-07-01",
                  "type": "Free",
                  "teams": {
                    "in": {
                      "id": 44,
                      "name": "Barcelona"
                    },
                    "out": {
                      "id": 33,
                      "name": "PSG"
                    }
                  }
                }
              ]
            }
          ]
        }
        """);
    }



}
