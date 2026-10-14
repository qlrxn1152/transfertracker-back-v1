package com.dhoon.transfertracker.internal.transfer.query.service.impl;

import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.transfer.domain.Transfer;
import com.dhoon.transfertracker.internal.transfer.query.dto.response.TransferDetailResponseDto;
import com.dhoon.transfertracker.internal.transfer.query.dto.response.TransferResponseDto;
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
class TransferDetailQueryTest {


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
    @DisplayName("이적 ID를 이용해 이적 상세 정보를 조회할 수 있다.")
    void getTransfer_success() {

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


        Transfer savedTransfer = saveTransfer(
                saka,
                arsenal,
                chelsea,
                "Permanent",
                LocalDate.of(2026, 7, 1)
        );


        // when
        TransferDetailResponseDto response =
                transferQueryOrchestrationService.getTransfer(
                        savedTransfer.getId()
                );


        // then
        assertThat(response)
                .isNotNull();

        assertThat(response.getTransfer())
                .isNotNull();


        TransferResponseDto transfer =
                response.getTransfer();


        assertThat(transfer.getTransferId())
                .isEqualTo(
                        savedTransfer.getId()
                );

        assertThat(transfer.getPlayerId())
                .isEqualTo(
                        saka.getId()
                );

        assertThat(transfer.getPlayerName())
                .isEqualTo(
                        "Bukayo Saka"
                );

        assertThat(transfer.getPhotoUrl())
                .isEqualTo(
                        "https://media.api-sports.io/football/players/10.png"
                );


        assertThat(transfer.getInTeamId())
                .isEqualTo(
                        arsenal.getId()
                );

        assertThat(transfer.getInTeamName())
                .isEqualTo(
                        "Arsenal"
                );


        assertThat(transfer.getOutTeamId())
                .isEqualTo(
                        chelsea.getId()
                );

        assertThat(transfer.getOutTeamName())
                .isEqualTo(
                        "Chelsea"
                );


        assertThat(transfer.getType())
                .isEqualTo(
                        "Permanent"
                );

        assertThat(transfer.getDate())
                .isEqualTo(
                        LocalDate.of(2026, 7, 1)
                );
    }


    @Test
    @DisplayName("한글 이름이 존재하면 이적 상세 정보에서 displayName을 반환한다.")
    void getTransfer_withKoreanNames() {

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


        Transfer savedTransfer = saveTransfer(
                son,
                lafc,
                tottenham,
                "Transfer",
                LocalDate.of(2026, 8, 1)
        );


        // when
        TransferDetailResponseDto response =
                transferQueryOrchestrationService.getTransfer(
                        savedTransfer.getId()
                );


        // then
        TransferResponseDto transfer =
                response.getTransfer();


        assertThat(transfer.getPlayerName())
                .isEqualTo(
                        "손흥민"
                );

        assertThat(transfer.getInTeamName())
                .isEqualTo(
                        "LAFC"
                );

        assertThat(transfer.getOutTeamName())
                .isEqualTo(
                        "토트넘"
                );
    }


    @Test
    @DisplayName("조회한 이적 정보 이외의 다른 이적 정보는 반환하지 않는다.")
    void getTransfer_onlyRequestedTransfer() {

        // given
        Player saka = savePlayer(
                "Bukayo Saka",
                10L
        );

        Player odegaard = savePlayer(
                "Martin Odegaard",
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


        Transfer sakaTransfer = saveTransfer(
                saka,
                arsenal,
                chelsea,
                "Saka Transfer",
                LocalDate.of(2026, 7, 1)
        );


        saveTransfer(
                odegaard,
                chelsea,
                arsenal,
                "Odegaard Transfer",
                LocalDate.of(2025, 7, 1)
        );


        // when
        TransferDetailResponseDto response =
                transferQueryOrchestrationService.getTransfer(
                        sakaTransfer.getId()
                );


        // then
        TransferResponseDto transfer =
                response.getTransfer();


        assertThat(transfer.getTransferId())
                .isEqualTo(
                        sakaTransfer.getId()
                );

        assertThat(transfer.getPlayerName())
                .isEqualTo(
                        "Bukayo Saka"
                );

        assertThat(transfer.getType())
                .isEqualTo(
                        "Saka Transfer"
                );
    }


    // ==================================================
    // 실패
    // ==================================================

    @Test
    @DisplayName("transferId가 null이면 예외가 발생한다.")
    void getTransfer_nullTransferId() {

        // when & then
        assertThatThrownBy(() ->
                transferQueryOrchestrationService.getTransfer(
                        null
                )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessage(
                        "이적 ID를 확인해 주세요."
                );
    }


    @Test
    @DisplayName("존재하지 않는 이적 ID를 조회하면 예외가 발생한다.")
    void getTransfer_notFound() {

        // given
        Long transferId = 999999L;


        // when & then
        assertThatThrownBy(() ->
                transferQueryOrchestrationService.getTransfer(
                        transferId
                )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessage(
                        "이적 기록을 찾을 수 없습니다."
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