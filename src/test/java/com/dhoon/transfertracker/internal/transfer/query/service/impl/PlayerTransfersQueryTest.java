package com.dhoon.transfertracker.internal.transfer.query.service.impl;

import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.exception.InvalidPlayerIdException;
import com.dhoon.transfertracker.internal.player.exception.NotFoundPlayerException;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.transfer.domain.Transfer;
import com.dhoon.transfertracker.internal.transfer.query.dto.response.PlayerTransferItemResponseDto;
import com.dhoon.transfertracker.internal.transfer.query.dto.response.PlayerTransfersResponseDto;
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
        TransferQueryOrchestrationService.class,
        TransferQueryTxService.class
})
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PlayerTransfersQueryTest {

    @Autowired
    TransferQueryOrchestrationService transferQueryOrchestrationService;

    @Autowired
    TransferRepository transferRepository;

    @Autowired
    PlayerRepository playerRepository;

    @Autowired
    TeamRepository teamRepository;


    @AfterEach
    void clearDatabase() {
        transferRepository.deleteAll();
        playerRepository.deleteAll();
        teamRepository.deleteAll();
    }


    // ==================================================
    // 정상
    // ==================================================

    @Test
    @DisplayName("특정 선수의 이적 정보를 조회할 수 있다.")
    void getPlayerTransfers_success() {

        // given
        Player saka = savePlayer(
                "Bukayo Saka",
                10L
        );

        Team arsenal = saveTeam(
                "Arsenal",
                42L
        );

        Team chelsea = saveTeam(
                "Chelsea",
                49L
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
                transferQueryOrchestrationService.getPlayerTransfers(
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
    @DisplayName("선수의 여러 이적 정보는 이적 날짜 최신순으로 조회된다.")
    void getPlayerTransfers_sortByTransferDateDesc() {

        // given
        Player saka = savePlayer(
                "Bukayo Saka",
                10L
        );

        Team arsenal = saveTeam(
                "Arsenal",
                42L
        );

        Team chelsea = saveTeam(
                "Chelsea",
                49L
        );

        Team barcelona = saveTeam(
                "Barcelona",
                529L
        );


        saveTransfer(
                saka,
                arsenal,
                barcelona,
                "2022 Transfer",
                LocalDate.of(2022, 7, 1)
        );

        saveTransfer(
                saka,
                chelsea,
                arsenal,
                "2026 Transfer",
                LocalDate.of(2026, 7, 1)
        );

        saveTransfer(
                saka,
                barcelona,
                chelsea,
                "2024 Transfer",
                LocalDate.of(2024, 7, 1)
        );


        // when
        PlayerTransfersResponseDto response =
                transferQueryOrchestrationService.getPlayerTransfers(
                        saka.getId()
                );


        // then
        assertThat(response.getPlayerTransfers())
                .extracting(PlayerTransferItemResponseDto::getDate)
                .containsExactly(
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2024, 7, 1),
                        LocalDate.of(2022, 7, 1)
                );
    }


    @Test
    @DisplayName("이적 날짜가 같으면 ID가 큰 이적 정보가 먼저 조회된다.")
    void getPlayerTransfers_sameDate_sortByIdDesc() {

        // given
        Player saka = savePlayer(
                "Bukayo Saka",
                10L
        );

        Team arsenal = saveTeam(
                "Arsenal",
                42L
        );

        Team chelsea = saveTeam(
                "Chelsea",
                49L
        );

        Team barcelona = saveTeam(
                "Barcelona",
                529L
        );


        LocalDate sameDate =
                LocalDate.of(2026, 7, 1);


        saveTransfer(
                saka,
                chelsea,
                arsenal,
                "First",
                sameDate
        );

        saveTransfer(
                saka,
                barcelona,
                chelsea,
                "Second",
                sameDate
        );


        // when
        PlayerTransfersResponseDto response =
                transferQueryOrchestrationService.getPlayerTransfers(
                        saka.getId()
                );


        // then
        assertThat(response.getPlayerTransfers())
                .extracting(PlayerTransferItemResponseDto::getType)
                .containsExactly(
                        "Second",
                        "First"
                );
    }


    @Test
    @DisplayName("요청한 선수 이외의 이적 정보는 포함하지 않는다.")
    void getPlayerTransfers_onlyRequestedPlayer() {

        // given
        Player saka = savePlayer(
                "Bukayo Saka",
                10L
        );

        Player son = savePlayer(
                "Son Heung-min",
                20L
        );


        Team arsenal = saveTeam(
                "Arsenal",
                42L
        );

        Team chelsea = saveTeam(
                "Chelsea",
                49L
        );


        saveTransfer(
                saka,
                arsenal,
                chelsea,
                "Saka Transfer",
                LocalDate.of(2026, 7, 1)
        );

        saveTransfer(
                son,
                chelsea,
                arsenal,
                "Son Transfer",
                LocalDate.of(2026, 8, 1)
        );


        // when
        PlayerTransfersResponseDto response =
                transferQueryOrchestrationService.getPlayerTransfers(
                        saka.getId()
                );


        // then
        assertThat(response.getPlayerTransfers())
                .hasSize(1);

        assertThat(response.getPlayerTransfers())
                .extracting(PlayerTransferItemResponseDto::getType)
                .containsExactly(
                        "Saka Transfer"
                );
    }


    @Test
    @DisplayName("한글 선수명과 팀명이 존재하면 displayName을 반환한다.")
    void getPlayerTransfers_withKoreanNames() {

        // given
        Player son = Player.of(
                "Son Heung-min",
                20L
        );

        son.assignKoPlayerName(
                "손흥민"
        );

        son = playerRepository.save(
                son
        );


        Team tottenham = Team.of(
                "Tottenham Hotspur",
                47L
        );

        tottenham.assignKoTeamName(
                "토트넘"
        );

        tottenham = teamRepository.save(
                tottenham
        );


        Team lafc = Team.of(
                "Los Angeles FC",
                1616L
        );

        lafc.assignKoTeamName(
                "LAFC"
        );

        lafc = teamRepository.save(
                lafc
        );


        saveTransfer(
                son,
                lafc,
                tottenham,
                "Transfer",
                LocalDate.of(2026, 8, 1)
        );


        // when
        PlayerTransfersResponseDto response =
                transferQueryOrchestrationService.getPlayerTransfers(
                        son.getId()
                );


        // then
        assertThat(response.getPlayerName())
                .isEqualTo("손흥민");


        PlayerTransferItemResponseDto transfer =
                response.getPlayerTransfers().get(0);


        assertThat(transfer.getInTeamName())
                .isEqualTo("LAFC");

        assertThat(transfer.getOutTeamName())
                .isEqualTo("토트넘");
    }


    // ==================================================
    // 경계
    // ==================================================

    @Test
    @DisplayName("선수는 존재하지만 이적 기록이 없으면 빈 리스트를 반환한다.")
    void getPlayerTransfers_emptyTransfers() {

        // given
        Player saka = savePlayer(
                "Bukayo Saka",
                10L
        );


        // when
        PlayerTransfersResponseDto response =
                transferQueryOrchestrationService.getPlayerTransfers(
                        saka.getId()
                );


        // then
        assertThat(response.getPlayerName())
                .isEqualTo("Bukayo Saka");

        assertThat(response.getPlayerTransfers())
                .isEmpty();
    }


    // ==================================================
    // 실패
    // ==================================================

    @Test
    @DisplayName("playerId가 null이면 예외가 발생한다.")
    void getPlayerTransfers_nullPlayerId() {

        assertThatThrownBy(() ->
                transferQueryOrchestrationService.getPlayerTransfers(
                        null
                )
        )
                .isInstanceOf(
                        InvalidPlayerIdException.class
                );
    }


    @Test
    @DisplayName("존재하지 않는 선수의 이적 정보를 조회하면 예외가 발생한다.")
    void getPlayerTransfers_playerNotFound() {

        // given
        Long playerId = 999999L;


        // when & then
        assertThatThrownBy(() ->
                transferQueryOrchestrationService.getPlayerTransfers(
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