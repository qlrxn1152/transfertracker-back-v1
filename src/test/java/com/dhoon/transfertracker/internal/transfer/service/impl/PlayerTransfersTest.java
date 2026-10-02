package com.dhoon.transfertracker.internal.transfer.service.impl;

import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.dto.response.PlayerItemResponseDto;
import com.dhoon.transfertracker.internal.player.exception.InvalidPlayerIdException;
import com.dhoon.transfertracker.internal.player.exception.NotFoundPlayerException;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.player.service.PlayerService;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.transfer.domain.Transfer;
import com.dhoon.transfertracker.internal.transfer.dto.response.PlayerTransferItemResponseDto;
import com.dhoon.transfertracker.internal.transfer.dto.response.PlayerTransfersResponseDto;
import com.dhoon.transfertracker.internal.transfer.repository.TransferRepository;
import com.dhoon.transfertracker.internal.transfer.service.TransferService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;


@DataJpaTest
@Import(TransferServiceImpl.class)
@ActiveProfiles("test")
class PlayerTransfersTest {

    @Autowired TransferService transferService;
    @Autowired TransferRepository transferRepository;
    @Autowired PlayerRepository playerRepository;
    @Autowired TeamRepository teamRepository;

    @MockitoBean PlayerService playerService;


    // ==================================================
    // 정상 상황
    // ==================================================

    @Test
    @DisplayName("특정 선수의 이적 정보를 조회할 수 있다.")
    void getPlayerTransfers_success() {

        // given
        Player saka =
                savePlayer("Bukayo Saka", 10L);

        Team arsenal =
                saveTeam("Arsenal", 1L);

        Team chelsea =
                saveTeam("Chelsea", 2L);

        saveTransfer(
                saka,
                arsenal,
                chelsea,
                "Transfer",
                LocalDate.of(2026, 7, 1)
        );

        /*
         * PlayerService가 실제로 호출됐다고 가정하고
         * 해당 선수 DTO를 반환하도록 Mock 동작 지정.
         */
        given(playerService.getPlayer(saka.getId()))
                .willReturn(
                        PlayerItemResponseDto.of(saka)
                );

        // when
        PlayerTransfersResponseDto response =
                transferService.getPlayerTransfers(
                        saka.getId()
                );

        // then
        assertThat(response.getPlayerName())
                .isEqualTo("Bukayo Saka");

        assertThat(response.getPlayerTransfers())
                .hasSize(1);

        PlayerTransferItemResponseDto transfer =
                response.getPlayerTransfers().get(0);

        assertThat(transfer.getInTeamName())
                .isEqualTo("Arsenal");

        assertThat(transfer.getOutTeamName())
                .isEqualTo("Chelsea");

        assertThat(transfer.getType())
                .isEqualTo("Transfer");

        assertThat(transfer.getDate())
                .isEqualTo(
                        LocalDate.of(2026, 7, 1)
                );
    }


    @Test
    @DisplayName("특정 선수의 여러 이적 정보는 최신순으로 조회된다.")
    void getPlayerTransfers_sortByTransferDateDesc() {

        // given
        Player saka =
                savePlayer("Bukayo Saka", 10L);

        Team arsenal =
                saveTeam("Arsenal", 1L);

        Team chelsea =
                saveTeam("Chelsea", 2L);

        Team barcelona =
                saveTeam("Barcelona", 3L);


        // 가장 오래된 이적
        saveTransfer(
                saka,
                barcelona,
                chelsea,
                "Transfer",
                LocalDate.of(2022, 7, 1)
        );

        // 가장 최신 이적
        saveTransfer(
                saka,
                chelsea,
                arsenal,
                "Transfer",
                LocalDate.of(2026, 7, 1)
        );

        // 중간 이적
        saveTransfer(
                saka,
                arsenal,
                barcelona,
                "Transfer",
                LocalDate.of(2024, 7, 1)
        );


        given(playerService.getPlayer(saka.getId()))
                .willReturn(
                        PlayerItemResponseDto.of(saka)
                );


        // when
        PlayerTransfersResponseDto response =
                transferService.getPlayerTransfers(
                        saka.getId()
                );


        // then
        assertThat(response.getPlayerTransfers())
                .hasSize(3);

        assertThat(response.getPlayerTransfers())
                .extracting(
                        PlayerTransferItemResponseDto::getDate
                )
                .containsExactly(
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2024, 7, 1),
                        LocalDate.of(2022, 7, 1)
                );
    }


    // 이적날짜 정렬 -> id ( PK ) 정렬 ( auto_increment )
    @Test
    @DisplayName("이적 날짜가 같으면 나중에 저장된 이적 정보가 먼저 조회된다.")
    void getPlayerTransfers_sameDate_sortByIdDesc() {

        // given
        Player saka =
                savePlayer("Bukayo Saka", 10L);

        Team arsenal =
                saveTeam("Arsenal", 1L);

        Team chelsea =
                saveTeam("Chelsea", 2L);

        Team barcelona =
                saveTeam("Barcelona", 3L);

        LocalDate sameDate =
                LocalDate.of(2026, 7, 1);


        /*
         * 먼저 저장
         * → 작은 Transfer ID --> 1L
         */
        saveTransfer(
                saka,
                chelsea,
                arsenal,
                "First Transfer",
                sameDate
        );


        /*
         * 나중에 저장
         * → 큰 Transfer ID --> 2L
         */
        saveTransfer(
                saka,
                barcelona,
                chelsea,
                "Second Transfer",
                sameDate
        );


        given(playerService.getPlayer(saka.getId()))
                .willReturn(
                        PlayerItemResponseDto.of(saka)
                );


        // when
        PlayerTransfersResponseDto response =
                transferService.getPlayerTransfers(
                        saka.getId()
                );


        // then
        assertThat(response.getPlayerTransfers())
                .extracting(
                        PlayerTransferItemResponseDto::getType
                )
                .containsExactly(
                        "Second Transfer",
                        "First Transfer"
                );

        assertThat(response.getPlayerTransfers())
                .extracting(PlayerTransferItemResponseDto::getInTeamName)
                .containsExactly("Barcelona", "Chelsea");
    }


    @Test
    @DisplayName("다른 선수의 이적 정보는 포함하지 않는다.")
    void getPlayerTransfers_onlyRequestedPlayer() {

        // given
        Player saka =
                savePlayer("Bukayo Saka", 10L);

        Player son =
                savePlayer("Son Heung-min", 20L);

        Team arsenal =
                saveTeam("Arsenal", 1L);

        Team chelsea =
                saveTeam("Chelsea", 2L);


        // Saka 이적
        saveTransfer(
                saka,
                chelsea,
                arsenal,
                "Saka Transfer",
                LocalDate.of(2026, 7, 1)
        );

        // Son 이적
        saveTransfer(
                son,
                arsenal,
                chelsea,
                "Son Transfer",
                LocalDate.of(2026, 8, 1)
        );


        given(playerService.getPlayer(saka.getId()))
                .willReturn(
                        PlayerItemResponseDto.of(saka)
                );


        // when
        PlayerTransfersResponseDto response =
                transferService.getPlayerTransfers(
                        saka.getId()
                );


        // then
        assertThat(response.getPlayerTransfers())
                .hasSize(1);

        assertThat(response.getPlayerTransfers())
                .extracting(
                        PlayerTransferItemResponseDto::getType
                )
                .containsExactly(
                        "Saka Transfer"
                );
    }


    // ==================================================
    // 예외 상황
    // ==================================================

    @Test
    @DisplayName("playerId가 null이면 선수 이적 정보를 조회할 수 없다.")
    void getPlayerTransfers_nullPlayerId_fail() {

        // given
        Long playerId = null;

        // when & then
        assertThatThrownBy(() ->
                transferService.getPlayerTransfers(
                        playerId
                )
        )
                .isInstanceOf(
                        InvalidPlayerIdException.class
                )
                .hasMessage(
                        "잘못된 PlayerId 값입니다."
                );
    }


    @Test
    @DisplayName("존재하지 않는 선수의 이적 정보는 조회할 수 없다.")
    void getPlayerTransfers_playerNotFound_fail() {

        // given
        Long playerId = 999L;

        given(playerService.getPlayer(playerId))
                .willThrow(
                        new NotFoundPlayerException()
                );


        // when & then
        assertThatThrownBy(() ->
                transferService.getPlayerTransfers(
                        playerId
                )
        )
                .isInstanceOf(
                        NotFoundPlayerException.class
                )
                .hasMessage(
                        "해당 Player 가 존재하지 않습니다."
                );
    }


    // ==================================================
    // 경계 상황
    // ==================================================

    @Test
    @DisplayName("선수는 존재하지만 이적 기록이 없으면 빈 리스트를 반환한다.")
    void getPlayerTransfers_emptyTransfers() {

        // given
        Player saka =
                savePlayer("Bukayo Saka", 10L);

        /*
         * Player는 DB에 존재하지만
         * Transfer는 저장하지 않는다.
         * 즉, 선수는 존재하지만 이적정보가 없는상황 ( 예를들어, 순수 아카데미 유망주 )
         */

        given(playerService.getPlayer(saka.getId()))
                .willReturn(
                        PlayerItemResponseDto.of(saka)
                );


        // when
        PlayerTransfersResponseDto response =
                transferService.getPlayerTransfers(
                        saka.getId()
                );


        // then
        assertThat(response.getPlayerName())
                .isEqualTo(
                        "Bukayo Saka"
                );

        assertThat(response.getPlayerTransfers())
                .isEmpty(); // 이적에 대한 데이터는 빈 리스트를 반환
    }


    // ==================================================
    // Fixture
    // ==================================================

    private Player savePlayer(
            String playerName,
            Long apiFootballId
    ) {

        return playerRepository.save(
                Player.of(
                        playerName,
                        apiFootballId
                )
        );
    }


    private Team saveTeam(
            String teamName,
            Long apiFootballId
    ) {

        return teamRepository.save(
                Team.of(
                        teamName,
                        apiFootballId
                )
        );
    }


    private Transfer saveTransfer(
            Player player,
            Team inTeam,
            Team outTeam,
            String transferType,
            LocalDate transferDate
    ) {

        return transferRepository.save(
                Transfer.of(
                        player,
                        inTeam,
                        outTeam,
                        transferType,
                        transferDate
                )
        );
    }
}