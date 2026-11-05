package com.dhoon.transfertracker.internal.transfer.query.service.impl;

import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.exception.InvalidPlayerIdException;
import com.dhoon.transfertracker.internal.player.query.dto.response.PlayerItemResponseDto;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.exception.InvalidTeamIdException;
import com.dhoon.transfertracker.internal.transfer.domain.Transfer;
import com.dhoon.transfertracker.internal.transfer.exception.InvalidTransferIdException;
import com.dhoon.transfertracker.internal.transfer.exception.InvalidTransferSearchPageValueException;
import com.dhoon.transfertracker.internal.transfer.query.dto.response.*;
import com.dhoon.transfertracker.internal.transfer.query.service.TransferService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class TransferQueryOrchestrationServiceTest {

    @Mock
    private TransferService txService;

    @InjectMocks
    private TransferQueryOrchestrationService transferQueryOrchestrationService;

    @Test
    @DisplayName("정상적인 선수 ID를 전달하면 TxService의 결과를 반환한다.")
    void getPlayerTransfers_success() {
        // given
        Player player = Player.of("testPlayer", 111L);
        ReflectionTestUtils.setField(player, "id", 1L);

        Team team1 = Team.of("team-1", 33L);
        Team team2 = Team.of("team-2", 33L);
        Team team3 = Team.of("team-3", 33L);

        Transfer transfer1 = Transfer.of(player, team2, team1, "55M", LocalDate.parse("2026-01-01"));
        Transfer transfer2 = Transfer.of(player, team3, team2, "55M", LocalDate.parse("2026-05-01"));

        PlayerTransfersResponseDto expected = PlayerTransfersResponseDto.of(player,
                List.of(PlayerTransferItemResponseDto.of(transfer1), PlayerTransferItemResponseDto.of(transfer2))
        );

        given(txService.getPlayerTransfers(1L))
                .willReturn(expected);

        // when
        PlayerTransfersResponseDto response = transferQueryOrchestrationService.getPlayerTransfers(1L);

        // then
        assertThat(response).isSameAs(expected);
        verify(txService, times(1)).getPlayerTransfers(1L);
    }

    @Test
    @DisplayName("선수 ID가 null이면 예외가 발생하고 TxService를 호출하지 않는다.")
    void getPlayerTransfers_invalidPlayerId() {
        // when & then
        assertThatThrownBy(() -> transferQueryOrchestrationService.getPlayerTransfers(null))
                .isInstanceOf(InvalidPlayerIdException.class);

        verifyNoInteractions(txService);
    }

    @Test
    @DisplayName("정상적인 이적 ID를 전달하면 TxService의 결과를 반환한다.")
    void getTransfer_success() {
        // given
        Player player = Player.of("testPlayer", 111L);
        ReflectionTestUtils.setField(player, "id", 1L);

        Team team1 = Team.of("team-1", 33L);
        Team team2 = Team.of("team-2", 33L);

        Transfer transfer = Transfer.of(player, team2, team1, "55M", LocalDate.parse("2026-01-01"));

        TransferDetailResponseDto expected = TransferDetailResponseDto.of(TransferResponseDto.of(transfer));

        given(txService.getTransfer(1L))
                .willReturn(expected);

        // when
        TransferDetailResponseDto response = transferQueryOrchestrationService.getTransfer(1L);

        // then
        assertThat(response).isSameAs(expected);
        verify(txService, times(1)).getTransfer(1L);
    }

    @Test
    @DisplayName("이적 ID가 null이면 예외가 발생하고 TxService를 호출하지 않는다.")
    void getTransfer_invalidTransferId() {
        // when & then
        assertThatThrownBy(() -> transferQueryOrchestrationService.getTransfer(null))
                .isInstanceOf(InvalidTransferIdException.class);

        verifyNoInteractions(txService);
    }

    // ========


    @Test
    @DisplayName("전체 이적 조회 시 페이지, 정렬, 검색 조건을 TxService에 전달한다.")
    void getTransfers_success() {
        // given
        int page = 1;
        String keyWord = "Messi";
        LeagueCode leagueCode = LeagueCode.LA_LIGA;
        Long teamId = 44L;

        AllTransfersResponseDto expected =
                AllTransfersResponseDto.of(List.of(), false, false);

        given(txService.getTransfers(
                any(Pageable.class),
                eq(keyWord),
                eq(leagueCode),
                eq(teamId)
        )).willReturn(expected);

        // when
        AllTransfersResponseDto response =
                transferQueryOrchestrationService.getTransfers(
                        page, keyWord, leagueCode, teamId
                );

        // then
        assertThat(response).isSameAs(expected);

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(txService, times(1)).getTransfers(
                pageableCaptor.capture(),
                eq(keyWord),
                eq(leagueCode),
                eq(teamId)
        );

        Pageable capturedPageable = pageableCaptor.getValue();

        assertThat(capturedPageable.getPageNumber()).isEqualTo(1);
        assertThat(capturedPageable.getPageSize()).isEqualTo(50);
        assertThat(capturedPageable.getOffset()).isEqualTo(50L);

        assertThat(capturedPageable.getSort().getOrderFor("transferDate"))
                .isNotNull()
                .extracting(Sort.Order::getDirection)
                .isEqualTo(Sort.Direction.DESC);

        assertThat(capturedPageable.getSort().getOrderFor("id"))
                .isNotNull()
                .extracting(Sort.Order::getDirection)
                .isEqualTo(Sort.Direction.DESC);

        assertThat(capturedPageable.getSort().toList())
                .extracting(Sort.Order::getProperty)
                .containsExactly("transferDate", "id");
    }

    @Test
    @DisplayName("전체 이적 조회 시 검색어 앞뒤 공백을 제거한다.")
    void getTransfers_trimKeyword() {
        // given
        String keyWord = "  Messi  ";
        Pageable expectedPageable = PageRequest.of(
                0,
                50,
                Sort.by(
                        Sort.Order.desc("transferDate"),
                        Sort.Order.desc("id")
                )
        );

        // when
        transferQueryOrchestrationService.getTransfers(
                0, keyWord, null, null
        );

        // then
        verify(txService, times(1)).getTransfers(
                expectedPageable,
                "Messi",
                null,
                null
        );
    }

    @Test
    @DisplayName("전체 이적 조회 시 검색어가 null이면 빈 문자열로 변환한다.")
    void getTransfers_nullKeyword() {
        // when
        transferQueryOrchestrationService.getTransfers(
                0, null, null, null
        );

        // then
        verify(txService, times(1)).getTransfers(
                any(Pageable.class),
                eq(""),
                isNull(),
                isNull()
        );
    }

    @Test
    @DisplayName("전체 이적 조회 시 페이지가 음수이면 예외가 발생한다.")
    void getTransfers_invalidPage() {
        // when & then
        assertThatThrownBy(
                () -> transferQueryOrchestrationService.getTransfers(
                        -1, "", null, null
                )
        ).isInstanceOf(InvalidTransferSearchPageValueException.class);

        verifyNoInteractions(txService);
    }

    @Test
    @DisplayName("팀별 이적 조회 시 페이지, 정렬, 검색 조건을 TxService에 전달한다.")
    void getTeamTransfers_success() {
        // given
        Long teamId = 44L;
        int page = 2;
        String keyWord = "Messi";

        AllTransfersResponseDto expected =
                AllTransfersResponseDto.of(List.of(), false, false);

        given(txService.getTeamTransfers(
                eq(teamId),
                any(Pageable.class),
                eq(keyWord)
        )).willReturn(expected);

        // when
        AllTransfersResponseDto response =
                transferQueryOrchestrationService.getTeamTransfers(
                        teamId, page, keyWord
                );

        // then
        assertThat(response).isSameAs(expected);

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(txService, times(1)).getTeamTransfers(
                eq(teamId),
                pageableCaptor.capture(),
                eq(keyWord)
        );

        Pageable capturedPageable = pageableCaptor.getValue();

        assertThat(capturedPageable.getPageNumber()).isEqualTo(2);
        assertThat(capturedPageable.getPageSize()).isEqualTo(50);
        assertThat(capturedPageable.getOffset()).isEqualTo(100L);

        assertThat(capturedPageable.getSort().toList())
                .extracting(Sort.Order::getProperty)
                .containsExactly("transferDate", "id");

        assertThat(capturedPageable.getSort().toList())
                .extracting(Sort.Order::getDirection)
                .containsExactly(
                        Sort.Direction.DESC,
                        Sort.Direction.DESC
                );
    }

    @Test
    @DisplayName("팀별 이적 조회 시 검색어 앞뒤 공백을 제거한다.")
    void getTeamTransfers_trimKeyword() {
        // given
        Long teamId = 44L;
        Pageable expectedPageable = PageRequest.of(
                0,
                50,
                Sort.by(
                        Sort.Order.desc("transferDate"),
                        Sort.Order.desc("id")
                )
        );

        // when
        transferQueryOrchestrationService.getTeamTransfers(
                teamId, 0, "  Messi  "
        );

        // then
        verify(txService, times(1)).getTeamTransfers(
                teamId,
                expectedPageable,
                "Messi"
        );
    }

    @Test
    @DisplayName("팀별 이적 조회 시 검색어가 null이면 빈 문자열로 변환한다.")
    void getTeamTransfers_nullKeyword() {
        // when
        transferQueryOrchestrationService.getTeamTransfers(
                44L, 0, null
        );

        // then
        verify(txService, times(1)).getTeamTransfers(
                eq(44L),
                any(Pageable.class),
                eq("")
        );
    }

    @Test
    @DisplayName("팀별 이적 조회 시 잘못된 페이지나 팀 ID면 예외가 발생한다.")
    void getTeamTransfers_invalidArguments() {
        // 음수 페이지
        assertThatThrownBy(
                () -> transferQueryOrchestrationService.getTeamTransfers(
                        44L, -1, ""
                )
        ).isInstanceOf(InvalidTransferSearchPageValueException.class);

        // null 팀 ID
        assertThatThrownBy(
                () -> transferQueryOrchestrationService.getTeamTransfers(
                        null, 0, ""
                )
        ).isInstanceOf(InvalidTeamIdException.class);

        verifyNoInteractions(txService);
    }





}
