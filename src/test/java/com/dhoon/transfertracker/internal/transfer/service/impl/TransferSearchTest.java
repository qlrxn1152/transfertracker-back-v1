package com.dhoon.transfertracker.internal.transfer.service.impl;

import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.player.service.PlayerService;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.team.service.TeamService;
import com.dhoon.transfertracker.internal.transfer.domain.Transfer;
import com.dhoon.transfertracker.internal.transfer.dto.response.AllTransfersResponseDto;
import com.dhoon.transfertracker.internal.transfer.dto.response.TransferResponseDto;
import com.dhoon.transfertracker.internal.transfer.exception.InvalidTransferSearchPageValueException;
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
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@DataJpaTest
@Import(TransferServiceImpl.class)
@ActiveProfiles("test")
class TransferSearchTest {

    @Autowired TransferService transferService;
    @Autowired TransferRepository transferRepository;
    @Autowired PlayerRepository playerRepository;
    @Autowired TeamRepository teamRepository;

    @MockitoBean PlayerService playerService;
    @MockitoBean TeamService teamService;



    // ==================================================
    // 정상 상황
    // ==================================================

    @Test
    @DisplayName("선수 이름의 일부를 이용해서 이적 정보를 검색할 수 있다.")
    void searchTransfer_byPartialPlayerName() {

        // given
        Team arsenal = saveTeam("Arsenal", 1L);
        Team chelsea = saveTeam("Chelsea", 2L);

        Player saka = savePlayer(
                "Bukayo Saka",
                10L
        );

        Player son = savePlayer(
                "Son Heung-min",
                20L
        );

        saveTransfer(
                saka,
                chelsea,
                arsenal,
                "Transfer",
                LocalDate.of(2026, 7, 1)
        );

        saveTransfer(
                son,
                arsenal,
                chelsea,
                "Transfer",
                LocalDate.of(2026, 6, 1)
        );

        // when
        AllTransfersResponseDto response =
                transferService.getTransfers(
                        0,
                        "Saka"
                );

        // then
        assertThat(response.getTransfers())
                .hasSize(1);

        assertThat(response.getTransfers())
                .extracting(TransferResponseDto::getPlayerName)
                .containsExactly("Bukayo Saka");
    }


    @Test
    @DisplayName("선수 이름의 대소문자를 구분하지 않고 이적 정보를 검색한다.")
    void searchTransfer_ignoreCase() {

        // given
        Team arsenal = saveTeam("Arsenal", 1L);
        Team chelsea = saveTeam("Chelsea", 2L);

        Player saka = savePlayer(
                "Bukayo Saka",
                10L
        );

        saveTransfer(
                saka,
                chelsea,
                arsenal,
                "Transfer",
                LocalDate.of(2026, 7, 1)
        );

        // when
        AllTransfersResponseDto response =
                transferService.getTransfers(
                        0,
                        "sAkA"
                );

        // then
        assertThat(response.getTransfers())
                .extracting(TransferResponseDto::getPlayerName)
                .containsExactly("Bukayo Saka");
    }


    @Test
    @DisplayName("한 선수에게 여러 이적 정보가 존재하면 모두 조회한다.")
    void searchTransfer_multipleTransfersOfSamePlayer() {

        // given
        Team arsenal = saveTeam("Arsenal", 1L);
        Team chelsea = saveTeam("Chelsea", 2L);
        Team barcelona = saveTeam("Barcelona", 3L);

        Player saka = savePlayer(
                "Bukayo Saka",
                10L
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
                barcelona,
                chelsea,
                "Transfer",
                LocalDate.of(2025, 7, 1)
        );

        // when
        AllTransfersResponseDto response =
                transferService.getTransfers(
                        0,
                        "Saka"
                );

        // then
        assertThat(response.getTransfers())
                .hasSize(2);

        assertThat(response.getTransfers())
                .extracting(TransferResponseDto::getPlayerName)
                .containsExactly(
                        "Bukayo Saka",
                        "Bukayo Saka"
                );

        assertThat(response.getTransfers())
                .extracting(TransferResponseDto::getDate)
                .containsExactly(
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2025, 7, 1)
                );

    }


    @Test
    @DisplayName("조회한 이적 정보에 선수와 이적 팀 정보가 정상적으로 포함된다.")
    void searchTransfer_mappingAssociatedEntities() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                1L
        );

        Team chelsea = saveTeam(
                "Chelsea",
                2L
        );

        Player saka = savePlayer(
                "Bukayo Saka",
                10L
        );

        saveTransfer(
                saka,
                chelsea,
                arsenal,
                "Transfer",
                LocalDate.of(2026, 7, 1)
        );

        // when
        AllTransfersResponseDto response =
                transferService.getTransfers(
                        0,
                        "Saka"
                );

        // then
        TransferResponseDto transfer =
                response.getTransfers().get(0);

        assertThat(transfer.getPlayerName())
                .isEqualTo("Bukayo Saka");

        assertThat(transfer.getPlayerId())
                .isEqualTo(saka.getId());

        assertThat(transfer.getOutTeamName())
                .isEqualTo("Chelsea");

        assertThat(transfer.getInTeamName())
                .isEqualTo("Arsenal");

        assertThat(transfer.getType())
                .isEqualTo("Transfer");

        assertThat(transfer.getDate())
                .isEqualTo(
                        LocalDate.of(2026, 7, 1)
                );

        assertThat(transfer.getPhotoUrl())
                .isEqualTo(
                        "https://media.api-sports.io/football/players/10.png"
                );
    }


    @Test
    @DisplayName("이적 정보는 이적 날짜의 내림차순으로 조회한다.")
    void searchTransfer_sortByTransferDateDesc() {

        // given
        Team arsenal = saveTeam("Arsenal", 1L);
        Team chelsea = saveTeam("Chelsea", 2L);

        Player saka =
                savePlayer("Bukayo Saka", 10L);

        Player son =
                savePlayer("Son Heung-min", 20L);

        Player bruno =
                savePlayer("Bruno Fernandes", 30L);

        saveTransfer(
                saka,
                chelsea,
                arsenal,
                "Transfer",
                LocalDate.of(2026, 1, 1)
        );

        saveTransfer(
                son,
                arsenal,
                chelsea,
                "Transfer",
                LocalDate.of(2026, 3, 1)
        );

        saveTransfer(
                bruno,
                chelsea,
                arsenal,
                "Transfer",
                LocalDate.of(2026, 2, 1)
        );

        // when
        AllTransfersResponseDto response =
                transferService.getTransfers(
                        0,
                        ""
                );

        // then
        assertThat(response.getTransfers())
                .extracting(TransferResponseDto::getPlayerName)
                .containsExactly(
                        "Son Heung-min",
                        "Bruno Fernandes",
                        "Bukayo Saka"
                );
    }


    // 날짜가 같다면, PK 에 대한 내림차순으로 정리되므로, 가장 최근에 저장된 이적정보가 먼저조회.
    @Test
    @DisplayName("이적 날짜가 같으면 나중에 저장된 이적 정보가 먼저 조회된다.")
    void searchTransfer_sameDate_sortByIdDesc() {

        // given
        Team arsenal = saveTeam("Arsenal", 1L);
        Team chelsea = saveTeam("Chelsea", 2L);

        Player saka =
                savePlayer("Bukayo Saka", 10L);

        Player son =
                savePlayer("Son Heung-min", 20L);

        LocalDate sameDate =
                LocalDate.of(2026, 7, 1);

        /*
         * 먼저 저장
         * → 작은 transfer id // 1L
         */
        saveTransfer(
                saka,
                chelsea,
                arsenal,
                "Transfer",
                sameDate
        );

        /*
         * 나중에 저장
         * → 큰 transfer id // 2L
         */
        saveTransfer(
                son,
                arsenal,
                chelsea,
                "Transfer",
                sameDate
        );

        // when
        AllTransfersResponseDto response =
                transferService.getTransfers(
                        0,
                        ""
                );

        // then
        assertThat(response.getTransfers())
                .extracting(TransferResponseDto::getPlayerName)
                .containsExactly(
                        "Son Heung-min",
                        "Bukayo Saka"
                );
    }


    @Test
    @DisplayName("검색 결과가 50개를 초과하면 다음 페이지가 존재한다.")
    void searchTransfer_paging() {

        // given

        IntStream.rangeClosed(1, 51)
                .forEach(i -> {
                            Player player = savePlayer("Test Player" + i, Long.valueOf(i));
                            Team inTeam = saveTeam("inteam" + i, Long.valueOf(i+100));
                            Team outTeam = saveTeam("outteam" + i, Long.valueOf(i));
                            saveTransfer(player, inTeam, outTeam, "transfer", LocalDate.of(2026, 2, 3));
                        }
                );
        // when
        AllTransfersResponseDto firstPage = // 1개 ~ 50개까지의 이적데이터
                transferService.getTransfers(
                        0,
                        "Test"
                );

        AllTransfersResponseDto secondPage = // 51개 ~
                transferService.getTransfers(
                        1,
                        "Test"
                );

        // then
        assertThat(firstPage.getTransfers())
                .hasSize(50);

        assertThat(firstPage.isHasNext())
                .isTrue();

        assertThat(firstPage.isHasPrevious())
                .isFalse();


        assertThat(secondPage.getTransfers())
                .hasSize(1);

        assertThat(secondPage.isHasNext())
                .isFalse();

        assertThat(secondPage.isHasPrevious())
                .isTrue();
    }


    // ==================================================
    // 예외 상황
    // ==================================================

    @Test
    @DisplayName("페이지 번호가 음수이면 이적 정보를 검색할 수 없다.")
    void searchTransfer_negativePage_fail() {

        // given
        int page = -1;

        // when & then
        assertThatThrownBy(() ->
                transferService.getTransfers(
                        page,
                        "Saka"
                )
        )
                .isInstanceOf(
                        InvalidTransferSearchPageValueException.class
                )
                .hasMessage(
                        "페이지 번호는 0 이상이어야 합니다."
                );
    }


    // ==================================================
    // 경계 상황
    // ==================================================

    @Test
    @DisplayName("검색어 앞뒤의 공백을 제거하고 이적 정보를 검색한다.")
    void searchTransfer_trimKeyword() {

        // given
        Team arsenal =
                saveTeam("Arsenal", 1L);

        Team chelsea =
                saveTeam("Chelsea", 2L);

        Player saka =
                savePlayer("Bukayo Saka", 10L);

        saveTransfer(
                saka,
                chelsea,
                arsenal,
                "Transfer",
                LocalDate.of(2026, 7, 1)
        );

        // when
        AllTransfersResponseDto response =
                transferService.getTransfers(
                        0,
                        "   Saka   "
                );

        // then
        assertThat(response.getTransfers())
                .extracting(
                        TransferResponseDto::getPlayerName
                )
                .containsExactly(
                        "Bukayo Saka"
                );
    }


    // 검색결과에 해당하는 데이터가 없다고하더라도 예외가 터지거나 하지않고, 빈 리스트를 반환하는게 정책입니다.
    @Test
    @DisplayName("검색 조건에 맞는 이적 정보가 없으면 빈 리스트를 반환한다.")
    void searchTransfer_notFound() {

        // given
        Team arsenal =
                saveTeam("Arsenal", 1L);

        Team chelsea =
                saveTeam("Chelsea", 2L);

        Player saka =
                savePlayer("Bukayo Saka", 10L);

        saveTransfer(
                saka,
                chelsea,
                arsenal,
                "Transfer",
                LocalDate.of(2026, 7, 1)
        );

        // when
        AllTransfersResponseDto response =
                transferService.getTransfers(
                        0,
                        "Messi"
                );

        // then
        assertThat(response.getTransfers())
                .isEmpty();

        assertThat(response.isHasNext())
                .isFalse();

        assertThat(response.isHasPrevious())
                .isFalse();
    }


    @Test
    @DisplayName("검색어가 공백이면 모든 이적 정보를 조회한다.")
    void searchTransfer_blankKeyword_findAll() {

        // given
        Team arsenal =
                saveTeam("Arsenal", 1L);

        Team chelsea =
                saveTeam("Chelsea", 2L);

        Player saka =
                savePlayer("Bukayo Saka", 10L);

        Player son =
                savePlayer("Son Heung-min", 20L);

        saveTransfer(
                saka,
                chelsea,
                arsenal,
                "Transfer",
                LocalDate.of(2026, 7, 1)
        );

        saveTransfer(
                son,
                arsenal,
                chelsea,
                "Transfer",
                LocalDate.of(2026, 6, 1)
        );

        // when
        AllTransfersResponseDto response =
                transferService.getTransfers(
                        0,
                        "   "
                );

        // then
        assertThat(response.getTransfers())
                .hasSize(2);

        assertThat(response.getTransfers())
                .extracting(TransferResponseDto::getPlayerName)
                .containsExactly(
                        "Bukayo Saka",
                        "Son Heung-min"
                );
    }


    // 검색어가 Null 인 경우, Service 코드쪽에서 -> "" 로 변환합니다. ( 공백문자 )
    @Test
    @DisplayName("검색어가 null이면 모든 이적 정보를 조회한다.")
    void searchTransfer_nullKeyword_findAll() {

        // given
        Team arsenal =
                saveTeam("Arsenal", 1L);

        Team chelsea =
                saveTeam("Chelsea", 2L);

        Player saka =
                savePlayer("Bukayo Saka", 10L);

        Player son =
                savePlayer("Son Heung-min", 20L);

        saveTransfer(
                saka,
                chelsea,
                arsenal,
                "Transfer",
                LocalDate.of(2026, 7, 1)
        );

        saveTransfer(
                son,
                arsenal,
                chelsea,
                "Transfer",
                LocalDate.of(2026, 6, 1)
        );

        // when
        AllTransfersResponseDto response =
                transferService.getTransfers(
                        0,
                        null
                );

        // then
        assertThat(response.getTransfers())
                .hasSize(2);
    }


    @Test
    @DisplayName("이적 정보가 정확히 50개이면 다음 페이지가 존재하지 않는다.")
    void searchTransfer_exactPageSize() {

        // given
        IntStream.rangeClosed(1, 50)
                .forEach(i -> {
                            Player player = savePlayer("player" + i, Long.valueOf(i));
                            Team inTeam = saveTeam("inteam" + i, Long.valueOf(i+100));
                            Team outTeam = saveTeam("outteam" + i, Long.valueOf(i));
                            saveTransfer(player, inTeam, outTeam, "transfer", LocalDate.of(2026, 2, 3));
                        }
                        );


        // when
        AllTransfersResponseDto response =
                transferService.getTransfers(
                        0,
                        ""
                );

        // then
        assertThat(response.getTransfers())
                .hasSize(50);

        assertThat(response.isHasNext())
                .isFalse();

        assertThat(response.isHasPrevious())
                .isFalse();
    }


    @Test
    @DisplayName("존재하지 않는 페이지를 조회하면 빈 리스트를 반환한다.")
    void searchTransfer_pageOutOfRange() {

        // given
        Team arsenal =
                saveTeam("Arsenal", 1L);

        Team chelsea =
                saveTeam("Chelsea", 2L);

        Player saka =
                savePlayer("Bukayo Saka", 10L);

        saveTransfer(
                saka,
                chelsea,
                arsenal,
                "Transfer",
                LocalDate.of(2026, 7, 1)
        ); // 이적정보 1건 -> 이적페이지는 1개. ( 20개 단위로 페이지가 1개니까. )

        // when
        AllTransfersResponseDto response =
                transferService.getTransfers(
                        100,
                        ""
                );

        // then
        assertThat(response.getTransfers())
                .isEmpty();

        assertThat(response.isHasNext())
                .isFalse();

        assertThat(response.isHasPrevious())
                .isTrue();
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
            Team outTeam,
            Team inTeam,
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