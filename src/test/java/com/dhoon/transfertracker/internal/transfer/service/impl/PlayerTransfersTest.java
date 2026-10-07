package com.dhoon.transfertracker.internal.transfer.service.impl;

import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.exception.InvalidPlayerIdException;
import com.dhoon.transfertracker.internal.player.exception.NotFoundPlayerException;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.transfer.domain.Transfer;
import com.dhoon.transfertracker.internal.transfer.dto.response.PlayerTransferItemResponseDto;
import com.dhoon.transfertracker.internal.transfer.dto.response.PlayerTransfersResponseDto;
import com.dhoon.transfertracker.internal.transfer.repository.TransferRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@DataJpaTest
@Import({
        TransferOrchestrationService.class,
        TransferTxService.class
})
@ActiveProfiles("test")

/*
 * 실제 요청 흐름:
 *
 * Client
 *      ↓
 * Controller
 *      ↓
 * TransferOrchestrationService
 *      → Transaction X
 *      → playerId validation
 *      ↓
 * TransferTxService
 *      → @Transactional(readOnly = true)
 *      → Player 존재 확인
 *      → Transfer 조회
 *      → Lazy Player / Team 접근
 *      → DTO 변환
 *      ↓
 * Transaction 종료
 *      ↓
 * DTO 반환
 *
 *
 * 실제 Transaction 경계를 확인하기 위해
 * DataJpaTest의 기본 Test Transaction을 사용하지 않는다.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PlayerTransfersTest {

    @Autowired
    TransferOrchestrationService transferOrchestrationService;

    @Autowired
    TransferRepository transferRepository;

    @Autowired
    PlayerRepository playerRepository;

    @Autowired
    TeamRepository teamRepository;


    /*
     * Test Transaction이 없으므로
     * FK 자식부터 직접 제거한다.
     *
     * Transfer
     *  ├─ Player
     *  ├─ inTeam
     *  └─ outTeam
     */
    @AfterEach
    void clearDatabase() {

        transferRepository.deleteAll();
        playerRepository.deleteAll();
        teamRepository.deleteAll();
    }


    // ==================================================
    // 정상 상황
    // ==================================================

    @Test
    @DisplayName("특정 선수의 이적 정보를 조회할 수 있다.")
    void getPlayerTransfers_success() {

        // given
        Player saka =
                savePlayer(
                        "Bukayo Saka",
                        10L
                );

        Team arsenal =
                saveTeam(
                        "Arsenal",
                        1L
                );

        Team chelsea =
                saveTeam(
                        "Chelsea",
                        2L
                );

        saveTransfer(
                saka,
                arsenal,
                chelsea,
                "Transfer",
                LocalDate.of(2026, 7, 1)
        );


        // when
        PlayerTransfersResponseDto response =
                transferOrchestrationService.getPlayerTransfers(
                        saka.getId()
                );


        // then
        assertThat(response.getPlayerName())
                .isEqualTo(
                        "Bukayo Saka"
                );

        assertThat(response.getPlayerTransfers())
                .hasSize(1);

        PlayerTransferItemResponseDto transfer =
                response.getPlayerTransfers().get(0);

        assertThat(transfer.getInTeamName())
                .isEqualTo(
                        "Arsenal"
                );

        assertThat(transfer.getOutTeamName())
                .isEqualTo(
                        "Chelsea"
                );

        assertThat(transfer.getType())
                .isEqualTo(
                        "Transfer"
                );

        assertThat(transfer.getDate())
                .isEqualTo(
                        LocalDate.of(2026, 7, 1)
                );
    }


    /*
     * Transfer.inTeam / outTeam은 LAZY다.
     *
     * Repository fetch join
     *      ↓
     * TransferTxService Transaction
     *      ↓
     * PlayerTransferItemResponseDto 변환
     *      ↓
     * Transaction 종료
     *
     * 순서로 끝나야 한다.
     */
    @Test
    @DisplayName("선수 이적 정보는 Transaction 내부에서 DTO로 변환되어 반환된다.")
    void getPlayerTransfers_dtoMapping() {

        // given
        Player saka =
                savePlayer(
                        "Bukayo Saka",
                        10L
                );

        Team arsenal =
                saveTeam(
                        "Arsenal",
                        1L
                );

        Team chelsea =
                saveTeam(
                        "Chelsea",
                        2L
                );

        saveTransfer(
                saka,
                chelsea,
                arsenal,
                "Permanent",
                LocalDate.of(2026, 8, 1)
        );


        // when
        PlayerTransfersResponseDto response =
                transferOrchestrationService.getPlayerTransfers(
                        saka.getId()
                );


        // then
        PlayerTransferItemResponseDto transfer =
                response.getPlayerTransfers().get(0);

        assertThat(transfer.getInTeamName())
                .isEqualTo(
                        "Chelsea"
                );

        assertThat(transfer.getOutTeamName())
                .isEqualTo(
                        "Arsenal"
                );

        assertThat(transfer.getDate())
                .isEqualTo(
                        LocalDate.of(2026, 8, 1)
                );

        assertThat(transfer.getType())
                .isEqualTo(
                        "Permanent"
                );
    }


    @Test
    @DisplayName("선수의 여러 이적 정보는 이적 날짜 최신순으로 조회된다.")
    void getPlayerTransfers_sortByTransferDateDesc() {

        // given
        Player saka =
                savePlayer(
                        "Bukayo Saka",
                        10L
                );

        Team arsenal =
                saveTeam(
                        "Arsenal",
                        1L
                );

        Team chelsea =
                saveTeam(
                        "Chelsea",
                        2L
                );

        Team barcelona =
                saveTeam(
                        "Barcelona",
                        3L
                );


        saveTransfer(
                saka,
                barcelona,
                chelsea,
                "Transfer",
                LocalDate.of(2022, 7, 1)
        );

        saveTransfer(
                saka,
                chelsea,
                arsenal,
                "Transfer",
                LocalDate.of(2026, 7, 1)
        );

        saveTransfer(
                saka,
                arsenal,
                barcelona,
                "Transfer",
                LocalDate.of(2024, 7, 1)
        );


        // when
        PlayerTransfersResponseDto response =
                transferOrchestrationService.getPlayerTransfers(
                        saka.getId()
                );


        // then
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


    /*
     * Repository ORDER BY:
     *
     * transferDate DESC,
     * id DESC
     */
    @Test
    @DisplayName("이적 날짜가 같으면 나중에 저장된 이적 정보가 먼저 조회된다.")
    void getPlayerTransfers_sameDate_sortByIdDesc() {

        // given
        Player saka =
                savePlayer(
                        "Bukayo Saka",
                        10L
                );

        Team arsenal =
                saveTeam(
                        "Arsenal",
                        1L
                );

        Team chelsea =
                saveTeam(
                        "Chelsea",
                        2L
                );

        Team barcelona =
                saveTeam(
                        "Barcelona",
                        3L
                );

        LocalDate sameDate =
                LocalDate.of(2026, 7, 1);


        // 먼저 저장 → 작은 ID
        saveTransfer(
                saka,
                chelsea,
                arsenal,
                "First Transfer",
                sameDate
        );

        // 나중 저장 → 큰 ID
        saveTransfer(
                saka,
                barcelona,
                chelsea,
                "Second Transfer",
                sameDate
        );


        // when
        PlayerTransfersResponseDto response =
                transferOrchestrationService.getPlayerTransfers(
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
                .extracting(
                        PlayerTransferItemResponseDto::getInTeamName
                )
                .containsExactly(
                        "Barcelona",
                        "Chelsea"
                );
    }


    @Test
    @DisplayName("다른 선수의 이적 정보는 포함하지 않는다.")
    void getPlayerTransfers_onlyRequestedPlayer() {

        // given
        Player saka =
                savePlayer(
                        "Bukayo Saka",
                        10L
                );

        Player son =
                savePlayer(
                        "Son Heung-min",
                        20L
                );

        Team arsenal =
                saveTeam(
                        "Arsenal",
                        1L
                );

        Team chelsea =
                saveTeam(
                        "Chelsea",
                        2L
                );


        saveTransfer(
                saka,
                chelsea,
                arsenal,
                "Saka Transfer",
                LocalDate.of(2026, 7, 1)
        );

        saveTransfer(
                son,
                arsenal,
                chelsea,
                "Son Transfer",
                LocalDate.of(2026, 8, 1)
        );


        // when
        PlayerTransfersResponseDto response =
                transferOrchestrationService.getPlayerTransfers(
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


    /*
     * PlayerTransfersResponseDto
     *      → Player.getDisplayName()
     *
     * PlayerTransferItemResponseDto
     *      → Team.getDisplayName()
     *
     * 을 사용하는지 함께 검증한다.
     */
    @Test
    @DisplayName("한글 선수명과 한글 팀명이 존재하면 한글 이름을 반환한다.")
    void getPlayerTransfers_withKoreanNames() {

        // given
        Player son =
                Player.of(
                        "Son Heung-min",
                        20L
                );

        son.assignKoPlayerName(
                "손흥민"
        );

        son =
                playerRepository.save(
                        son
                );


        Team tottenham =
                Team.of(
                        "Tottenham",
                        1L
                );

        tottenham.assignKoTeamName(
                "토트넘"
        );

        tottenham =
                teamRepository.save(
                        tottenham
                );


        Team lafc =
                Team.of(
                        "Los Angeles FC",
                        2L
                );

        lafc.assignKoTeamName(
                "LAFC"
        );

        lafc =
                teamRepository.save(
                        lafc
                );


        saveTransfer(
                son,
                tottenham,
                lafc,
                "Transfer",
                LocalDate.of(2026, 8, 1)
        );


        // when
        PlayerTransfersResponseDto response =
                transferOrchestrationService.getPlayerTransfers(
                        son.getId()
                );


        // then
        assertThat(response.getPlayerName())
                .isEqualTo(
                        "손흥민"
                );

        PlayerTransferItemResponseDto transfer =
                response.getPlayerTransfers().get(0);

        assertThat(transfer.getInTeamName())
                .isEqualTo(
                        "토트넘"
                );

        assertThat(transfer.getOutTeamName())
                .isEqualTo(
                        "LAFC"
                );
    }


    // ==================================================
    // 경계 상황
    // ==================================================

    /*
     * Player는 존재하지만 Transfer가 하나도 없는 것은
     * 정상적인 상태다.
     */
    @Test
    @DisplayName("선수는 존재하지만 이적 기록이 없으면 빈 리스트를 반환한다.")
    void getPlayerTransfers_emptyTransfers() {

        // given
        Player saka =
                savePlayer(
                        "Bukayo Saka",
                        10L
                );


        // when
        PlayerTransfersResponseDto response =
                transferOrchestrationService.getPlayerTransfers(
                        saka.getId()
                );


        // then
        assertThat(response.getPlayerName())
                .isEqualTo(
                        "Bukayo Saka"
                );

        assertThat(response.getPlayerTransfers())
                .isEmpty();
    }


    // ==================================================
    // 실패 상황
    // ==================================================

    /*
     * DB 접근이 필요 없는 validation.
     *
     * TransferOrchestrationService에서 차단된다.
     */
    @Test
    @DisplayName("playerId가 null이면 선수 이적 정보를 조회할 수 없다.")
    void getPlayerTransfers_nullPlayerId() {

        // when & then
        assertThatThrownBy(() ->
                transferOrchestrationService.getPlayerTransfers(
                        null
                )
        )
                .isInstanceOf(
                        InvalidPlayerIdException.class
                );
    }


    /*
     * playerId는 유효한 형태지만
     * 실제 DB에 Player가 존재하지 않는다.
     *
     * TransferTxService에서 실패한다.
     */
    @Test
    @DisplayName("존재하지 않는 선수의 이적 정보는 조회할 수 없다.")
    void getPlayerTransfers_playerNotFound() {

        // given
        Long playerId = 999999L;


        // when & then
        assertThatThrownBy(() ->
                transferOrchestrationService.getPlayerTransfers(
                        playerId
                )
        )
                .isInstanceOf(
                        NotFoundPlayerException.class
                );
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