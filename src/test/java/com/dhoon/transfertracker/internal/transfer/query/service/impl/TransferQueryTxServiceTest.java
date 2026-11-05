package com.dhoon.transfertracker.internal.transfer.query.service.impl;

import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.exception.NotFoundPlayerException;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.exception.NotFoundTeamException;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.transfer.domain.Transfer;
import com.dhoon.transfertracker.internal.transfer.exception.NotFoundTransferException;
import com.dhoon.transfertracker.internal.transfer.query.dto.response.*;
import com.dhoon.transfertracker.internal.transfer.query.repository.TransferQueryRepository;
import com.dhoon.transfertracker.internal.transfer.query.service.TransferService;
import com.dhoon.transfertracker.internal.transfer.repository.TransferRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cglib.core.Local;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class TransferQueryTxServiceTest {

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TransferQueryRepository transferQueryRepository;

    @InjectMocks
    private TransferQueryTxService transferQueryTxService;

    @Test
    @DisplayName("선수의 이적 이력을 정상적으로 조회한다.")
    void getPlayerTransfers_success() {
        // given
        Player player = Player.of("testPlayer", 111L);
        ReflectionTestUtils.setField(player, "id", 1L);

        given(playerRepository.findById(1L))
                .willReturn(Optional.of(player));

        Team t1 = Team.of("team-1", 222L);
        Team t2 = Team.of("team-2", 333L);
        Team t3 = Team.of("team-3", 444L);

        Transfer transfer1 = Transfer.of(player, t1, t2, "55 M", LocalDate.parse("2026-07-01")); // t2 -> t1
        Transfer transfer2 = Transfer.of(player, t3, t1, "11 M", LocalDate.parse("2026-09-30")); // t1 -> t3

        given(transferRepository.findAllByPlayerId(1L))
                .willReturn(List.of(transfer2, transfer1));

        // when
        PlayerTransfersResponseDto response = transferQueryTxService.getPlayerTransfers(1L);

        // then
        assertThat(response.getPlayerName()).isEqualTo("testPlayer");
        assertThat(response.getPlayerTransfers()).hasSize(2);
        assertThat(response.getPlayerTransfers()).extracting(PlayerTransferItemResponseDto::getInTeamName)
                .containsExactly("team-3", "team-1");
        assertThat(response.getPlayerTransfers()).extracting(PlayerTransferItemResponseDto::getOutTeamName)
                .containsExactly("team-1", "team-2");
        assertThat(response.getPlayerTransfers()).extracting(PlayerTransferItemResponseDto::getDate)
                .containsExactly(LocalDate.parse("2026-09-30"), LocalDate.parse("2026-07-01"));

        verify(playerRepository, times(1)).findById(1L);
        verify(transferRepository, times(1)).findAllByPlayerId(1L);
    }

    @Test
    @DisplayName("선수는 존재하지만 이적 이력이 없으면 빈 목록을 반환한다.")
    void getPlayerTransfers_empty() {
        // given
        Player player = Player.of("testPlayer", 111L);
        ReflectionTestUtils.setField(player, "id", 1L);

        given(playerRepository.findById(1L))
                .willReturn(Optional.of(player));

        given(transferRepository.findAllByPlayerId(1L))
                .willReturn(List.of());

        // when
        PlayerTransfersResponseDto response = transferQueryTxService.getPlayerTransfers(1L);

        // then
        assertThat(response.getPlayerName()).isEqualTo("testPlayer");
        assertThat(response.getPlayerTransfers()).isEmpty();

        verify(playerRepository, times(1)).findById(1L);
        verify(transferRepository, times(1)).findAllByPlayerId(1L);
    }

    @Test
    @DisplayName("존재하지 않는 선수의 이적 이력을 조회하면 예외가 발생한다.")
    void getPlayerTransfers_notFound() {
        // given
        given(playerRepository.findById(1L))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> transferQueryTxService.getPlayerTransfers(1L))
                .isInstanceOf(NotFoundPlayerException.class);

        verify(playerRepository, times(1)).findById(1L);
        verifyNoInteractions(transferRepository);
    }

    @Test
    @DisplayName("이적 기록 ID로 이적 상세 정보를 정상적으로 조회한다.")
    void getTransfer_success() {
        // given
        Player player = Player.of("testPlayer", 111L);
        ReflectionTestUtils.setField(player, "id", 1L);


        Team t1 = Team.of("team-1", 222L);
        ReflectionTestUtils.setField(t1, "id", 55L);

        Team t2 = Team.of("team-2", 333L);
        ReflectionTestUtils.setField(t2, "id", 56L);

        Transfer transfer = Transfer.of(player, t1, t2, "55 M", LocalDate.parse("2026-07-01")); // t2 -> t1
        ReflectionTestUtils.setField(transfer, "id", 999L);

        given(transferRepository.findByIdWithLazyEntities(999L))
                .willReturn(Optional.of(transfer));

        // when
        TransferDetailResponseDto response = transferQueryTxService.getTransfer(999L);

        // then
        assertThat(response.getTransfer().getTransferId()).isEqualTo(999L);
        assertThat(response.getTransfer().getPlayerId()).isEqualTo(1L);
        assertThat(response.getTransfer().getInTeamId()).isEqualTo(55L);
        assertThat(response.getTransfer().getOutTeamId()).isEqualTo(56L);
        assertThat(response.getTransfer().getDate()).isEqualTo(LocalDate.parse("2026-07-01"));

        verify(transferRepository, times(1)).findByIdWithLazyEntities(999L);
    }

    @Test
    @DisplayName("존재하지 않는 이적 기록을 조회하면 예외가 발생한다.")
    void getTransfer_notFound() {
        // given
        given(transferRepository.findByIdWithLazyEntities(1L))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> transferQueryTxService.getTransfer(1L))
                .isInstanceOf(NotFoundTransferException.class);

        verify(transferRepository, times(1)).findByIdWithLazyEntities(1L);
    }

    @Test
    @DisplayName("전체 이적 기록을 정상적으로 조회한다.")
    void getTransfers_success() {
        // given
        Pageable pageable = PageRequest.of(0, 2); // 첫페이지에는 2개의 이적데이터가 보여짐.

        Player p1 = Player.of("player-1", 111L);

        Team t1 = Team.of("team-1", 333L);
        Team t2 = Team.of("team-2", 444L);
        Team t3 = Team.of("team-3", 555L);

        Transfer transfer1 = Transfer.of(p1, t2, t1, "55M", LocalDate.parse("2026-07-01"));// t1 -> t2
        ReflectionTestUtils.setField(transfer1, "id", 1L);

        Transfer transfer2 = Transfer.of(p1, t3, t2, "10M", LocalDate.parse("2026-09-30"));// t2 -> t3
        ReflectionTestUtils.setField(transfer2, "id", 2L);

        Page<Transfer> transferPage = new PageImpl<>(
                List.of(transfer2, transfer1),
                pageable,
                5
        ); // 총 이적 데이터는 5개이고, 요청한 1번째 페이지에는 2개의 이적데이터가 존재.

        given(transferQueryRepository.findTransfers(pageable, "", null, null))
                .willReturn(transferPage);

        // when
        AllTransfersResponseDto response = transferQueryTxService.getTransfers(pageable, "", null, null);

        // then
        assertThat(response.isHasNext()).isTrue();
        assertThat(response.isHasPrevious()).isFalse();

        assertThat(response.getTransfers()).hasSize(2);
        assertThat(response.getTransfers()).extracting(TransferResponseDto::getTransferId).containsExactly(2L, 1L);
        assertThat(response.getTransfers()).extracting(TransferResponseDto::getInTeamName).containsExactly("team-3", "team-2");
        assertThat(response.getTransfers()).extracting(TransferResponseDto::getOutTeamName).containsExactly("team-2", "team-1");

        verify(transferQueryRepository, times(1)).findTransfers(pageable, "", null, null);

    }

    @Test
    @DisplayName("전체 이적 기록이 없으면 빈 목록을 반환한다.")
    void getTransfers_empty() {
        // given
        Pageable pageable = PageRequest.of(0, 2);

        Page<Transfer> transferPage = Page.empty();

        given(transferQueryRepository.findTransfers(pageable, "", null, null))
                .willReturn(transferPage);

        // when
        AllTransfersResponseDto response = transferQueryTxService.getTransfers(pageable, "", null, null);

        // then
        assertThat(response.getTransfers()).isEmpty();
        assertThat(response.isHasNext()).isFalse();
        assertThat(response.isHasPrevious()).isFalse();

        verify(transferQueryRepository, times(1)).findTransfers(pageable, "", null, null);
    }

    @Test
    @DisplayName("이적 기록 조회 시 검색 조건을 Repository에 전달한다.")
    void getTransfers_withFilters() {
        // given
        Pageable pageable = PageRequest.of(1, 2); // 2번째페이지에 2개의 이적데이터.

        Player bruno = Player.of("Bruno", 111L);
        Player saka = Player.of("Saka", 222L);
        Player son = Player.of("Son", 333L);
        Player messi = Player.of("Messi", 444L);

        Team mu = Team.of("Manchester United", 33L);
        mu.assignTeamLeague(LeagueCode.EPL);

        Team barca = Team.of("Barcelona", 444L);
        ReflectionTestUtils.setField(barca, "id", 44L);
        barca.assignTeamLeague(LeagueCode.LA_LIGA);

        Team mct = Team.of("Manchester City", 55L);
        mct.assignTeamLeague(LeagueCode.EPL);

        Transfer t1 = Transfer.of(bruno, barca, mu, "1M", LocalDate.parse("2026-07-01"));
        Transfer t2 = Transfer.of(saka, barca, mu, "1M", LocalDate.parse("2026-09-30"));
        Transfer t3 = Transfer.of(son, mu, barca, "5M", LocalDate.parse("2026-09-15"));
        Transfer t4 = Transfer.of(messi, barca, mu, "444M", LocalDate.parse("2026-09-20"));
        Transfer t5 = Transfer.of(messi, mu, barca, "5M", LocalDate.parse("2029-01-03"));
        Transfer t6 = Transfer.of(messi, mu, mct, "1111M", LocalDate.parse("2026-09-30"));

        String keyWord = "Messi";
        LeagueCode leagueCode = LeagueCode.LA_LIGA;
        Long teamId = 44L;

        Page<Transfer> transferPage = new PageImpl<>(
                List.of(
                        t5,
                        t4
                ),
                pageable,
                6
        );

        given(transferQueryRepository.findTransfers(pageable, keyWord, leagueCode, teamId))
                .willReturn(transferPage);


        // when
        AllTransfersResponseDto response = transferQueryTxService.getTransfers(pageable, keyWord, leagueCode, teamId);

        // then
        assertThat(response.getTransfers()).hasSize(2);
        assertThat(response.isHasPrevious()).isTrue();
        assertThat(response.isHasNext()).isTrue();
        assertThat(response.getTransfers()).extracting(TransferResponseDto::getPlayerName).containsExactly("Messi", "Messi");
        assertThat(response.getTransfers()).extracting(TransferResponseDto::getInTeamName).containsExactly("Manchester United", "Barcelona");

        verify(transferQueryRepository, times(1)).findTransfers(pageable, keyWord, leagueCode, teamId);
    }

    @Test
    @DisplayName("특정 팀의 이적 기록을 정상적으로 조회한다.")
    void getTeamTransfers_success() {
        // given
        Long teamId = 1L;
        Pageable pageable = PageRequest.of(0, 2);
        String keyWord = "";

        Player player = Player.of("testPlayer", 1211L);

        Team team1 = Team.of("testTeam1", 444L);
        ReflectionTestUtils.setField(team1, "id", 1L);

        given(teamRepository.findById(1L))
                .willReturn(Optional.of(team1));

        Team team2 = Team.of("testTeam2", 123L);

        Transfer transfer1 = Transfer.of(player, team2, team1, "55M", LocalDate.parse("2026-07-01"));
        Transfer transfer2 = Transfer.of(player, team1, team2, "11M", LocalDate.parse("2028-11-30"));

        Page<Transfer> transferPage = new PageImpl<>(
                List.of(transfer2, transfer1),
                pageable,
                5
        );

        given(transferQueryRepository.findTransfers(pageable, keyWord, null, teamId))
                .willReturn(transferPage);

        // when
        AllTransfersResponseDto response = transferQueryTxService.getTeamTransfers(teamId, pageable, keyWord);

        // then
        assertThat(response.getTransfers()).hasSize(2);
        assertThat(response.isHasPrevious()).isFalse();
        assertThat(response.isHasNext()).isTrue();

        assertThat(response.getTransfers()).extracting(TransferResponseDto::getInTeamName)
                .containsExactly("testTeam1", "testTeam2");

        verify(teamRepository, times(1)).findById(1L);
        verify(transferQueryRepository, times(1)).findTransfers(pageable, keyWord, null, teamId);
    }

    @Test
    @DisplayName("팀은 존재하지만 이적 기록이 없으면 빈 목록을 반환한다.")
    void getTeamTransfers_empty() {
        // given
        Long teamId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        String keyWord = "";

        // 1. Team 생성
        Team team = Team.of("testTeam", 444L);
        ReflectionTestUtils.setField(team, "id", 1L);
        given(teamRepository.findById(1L))
                .willReturn(Optional.of(team));

        Page<Transfer> emptyPage = Page.empty();
        given(transferQueryRepository.findTransfers(pageable, keyWord, null, teamId))
                .willReturn(emptyPage);

        // when
        AllTransfersResponseDto response = transferQueryTxService.getTeamTransfers(teamId, pageable, keyWord);

        // then
        assertThat(response.getTransfers()).isEmpty();
        assertThat(response.isHasPrevious()).isFalse();
        assertThat(response.isHasNext()).isFalse();

        verify(teamRepository, times(1)).findById(1L);
        verify(transferQueryRepository, times(1)).findTransfers(pageable, keyWord, null, teamId);
    }

    @Test
    @DisplayName("팀은 존재하지 않은경우, NotFoundTeamException 이 발생한다.")
    void getTeamTransfers_notFoundTeam() {
        // given
        given(teamRepository.findById(1L))
                .willReturn(Optional.empty());

        Pageable pageable = PageRequest.of(0, 10);
        String keyWord = "";

        // when && then
        assertThatThrownBy(() -> transferQueryTxService.getTeamTransfers(1L, pageable, keyWord))
                .isInstanceOf(NotFoundTeamException.class);

        verify(teamRepository, times(1)).findById(1L);
        verifyNoInteractions(transferQueryRepository);
    }


}
