package com.dhoon.transfertracker.internal.transfer.service.impl;

import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.dto.response.PlayerItemResponseDto;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.player.service.PlayerService;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.exception.NotFoundTeamException;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.team.service.TeamService;
import com.dhoon.transfertracker.internal.transfer.domain.Transfer;
import com.dhoon.transfertracker.internal.transfer.dto.response.AllTransfersResponseDto;
import com.dhoon.transfertracker.internal.transfer.dto.response.TransferResponseDto;
import com.dhoon.transfertracker.internal.transfer.exception.InvalidTeamIdException;
import com.dhoon.transfertracker.internal.transfer.exception.InvalidTransferSearchPageValueException;
import com.dhoon.transfertracker.internal.transfer.repository.TransferRepository;
import com.dhoon.transfertracker.internal.transferpost.service.TransferService;
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
import static org.mockito.BDDMockito.given;


@DataJpaTest
@Import(TransferServiceImpl.class)
@ActiveProfiles("test")
class TeamTransfersTest {

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
    @DisplayName("해당 팀으로 들어온 선수의 이적 정보를 조회할 수 있다.")
    void getTeamTransfers_inTeam() {

        // given
        Team arsenal = saveTeam("Arsenal", 1L);
        Team chelsea = saveTeam("Chelsea", 2L);

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
                transferService.getTeamTransfers(
                        arsenal.getId(),
                        0,
                        ""
                );

        // then
        assertThat(response.getTransfers())
                .hasSize(1);

        TransferResponseDto transfer =
                response.getTransfers().get(0);

        assertThat(transfer.getPlayerName())
                .isEqualTo("Bukayo Saka");

        assertThat(transfer.getInTeamName())
                .isEqualTo("Arsenal");

        assertThat(transfer.getOutTeamName())
                .isEqualTo("Chelsea");
    }


    @Test
    @DisplayName("해당 팀에서 나간 선수의 이적 정보를 조회할 수 있다.")
    void getTeamTransfers_outTeam() {

        // given
        Team arsenal = saveTeam("Arsenal", 1L);
        Team barcelona = saveTeam("Barcelona", 2L);

        Player saka =
                savePlayer("Bukayo Saka", 10L);

        saveTransfer(
                saka,
                arsenal,
                barcelona,
                "Transfer",
                LocalDate.of(2026, 7, 1)
        );

        // when
        AllTransfersResponseDto response =
                transferService.getTeamTransfers(
                        arsenal.getId(),
                        0,
                        ""
                );

        // then
        assertThat(response.getTransfers())
                .hasSize(1);

        TransferResponseDto transfer =
                response.getTransfers().get(0);

        assertThat(transfer.getOutTeamName())
                .isEqualTo("Arsenal");

        assertThat(transfer.getInTeamName())
                .isEqualTo("Barcelona");
    }


    @Test
    @DisplayName("팀의 영입과 방출 이적을 모두 조회한다.")
    void getTeamTransfers_inAndOut() {

        // given
        Team arsenal = saveTeam("Arsenal", 1L);
        Team chelsea = saveTeam("Chelsea", 2L);
        Team barcelona = saveTeam("Barcelona", 3L);

        Player saka =
                savePlayer("Bukayo Saka", 10L);

        Player son =
                savePlayer("Son Heung-min", 20L);

        // Chelsea -> Arsenal
        saveTransfer(
                saka,
                chelsea,
                arsenal,
                "Transfer",
                LocalDate.of(2026, 7, 1)
        );

        // Arsenal -> Barcelona
        saveTransfer(
                son,
                arsenal,
                barcelona,
                "Transfer",
                LocalDate.of(2026, 6, 1)
        );

        // when
        AllTransfersResponseDto response =
                transferService.getTeamTransfers(
                        arsenal.getId(),
                        0,
                        ""
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


    @Test
    @DisplayName("해당 팀과 관계없는 이적 정보는 조회하지 않는다.")
    void getTeamTransfers_excludeOtherTeams() {

        // given
        Team arsenal = saveTeam("Arsenal", 1L);
        Team chelsea = saveTeam("Chelsea", 2L);
        Team barcelona = saveTeam("Barcelona", 3L);

        Player saka =
                savePlayer("Bukayo Saka", 10L);

        Player son =
                savePlayer("Son Heung-min", 20L);

        // Arsenal 관련
        saveTransfer(
                saka,
                chelsea,
                arsenal,
                "Arsenal Transfer",
                LocalDate.of(2026, 7, 1)
        );

        // Arsenal과 관계없음
        saveTransfer(
                son,
                chelsea,
                barcelona,
                "Other Transfer",
                LocalDate.of(2026, 8, 1)
        );

        // when
        AllTransfersResponseDto response =
                transferService.getTeamTransfers(
                        arsenal.getId(),
                        0,
                        ""
                );

        // then
        assertThat(response.getTransfers())
                .hasSize(1);

        assertThat(response.getTransfers())
                .extracting(TransferResponseDto::getType)
                .containsExactly("Arsenal Transfer");
    }


    @Test
    @DisplayName("팀 이적 정보에서 선수 이름의 일부로 검색할 수 있다.")
    void getTeamTransfers_searchByPlayerName() {

        // given
        Team arsenal = saveTeam("Arsenal", 1L);
        Team chelsea = saveTeam("Chelsea", 2L);

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
                transferService.getTeamTransfers(
                        arsenal.getId(),
                        0,
                        "Saka"
                );

        // then
        assertThat(response.getTransfers())
                .extracting(TransferResponseDto::getPlayerName)
                .containsExactly("Bukayo Saka");
    }


    @Test
    @DisplayName("팀 이적 검색은 선수 이름의 대소문자를 구분하지 않는다.")
    void getTeamTransfers_ignoreCase() {

        // given
        Team arsenal = saveTeam("Arsenal", 1L);
        Team chelsea = saveTeam("Chelsea", 2L);

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
                transferService.getTeamTransfers(
                        arsenal.getId(),
                        0,
                        "sAkA"
                );

        // then
        assertThat(response.getTransfers())
                .extracting(TransferResponseDto::getPlayerName)
                .containsExactly("Bukayo Saka");
    }


    @Test
    @DisplayName("팀 이적 정보는 이적 날짜의 내림차순으로 조회된다.")
    void getTeamTransfers_sortByTransferDateDesc() {

        // given
        Team arsenal = saveTeam("Arsenal", 1L);
        Team chelsea = saveTeam("Chelsea", 2L);
        Team barcelona = saveTeam("Barcelona", 3L);

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
                LocalDate.of(2024, 7, 1)
        );

        saveTransfer(
                son,
                arsenal,
                barcelona,
                "Transfer",
                LocalDate.of(2026, 7, 1)
        );

        saveTransfer(
                bruno,
                chelsea,
                arsenal,
                "Transfer",
                LocalDate.of(2025, 7, 1)
        );

        // when
        AllTransfersResponseDto response =
                transferService.getTeamTransfers(
                        arsenal.getId(),
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


    @Test
    @DisplayName("이적 날짜가 같으면 나중에 저장된 팀 이적 정보가 먼저 조회된다.")
    void getTeamTransfers_sameDate_sortByIdDesc() {

        // given
        Team arsenal = saveTeam("Arsenal", 1L);
        Team chelsea = saveTeam("Chelsea", 2L);

        Player saka =
                savePlayer("Bukayo Saka", 10L);

        Player son =
                savePlayer("Son Heung-min", 20L);

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
                son,
                arsenal,
                chelsea,
                "Second Transfer",
                sameDate
        );

        // when
        AllTransfersResponseDto response =
                transferService.getTeamTransfers(
                        arsenal.getId(),
                        0,
                        ""
                );

        // then
        assertThat(response.getTransfers())
                .extracting(TransferResponseDto::getType)
                .containsExactly(
                        "Second Transfer",
                        "First Transfer"
                );
    }


    @Test
    @DisplayName("팀 이적 검색 결과가 50개를 초과하면 다음 페이지가 존재한다.")
    void getTeamTransfers_paging() {

        // given
        Team arsenal = saveTeam("Arsenal", 1L);
        Team chelsea = saveTeam("Chelsea", 2L);

        /*
         * Transfer UNIQUE 제약:
         *
         * player_id + in_team_id + out_team_id + transfer_date
         *
         * 따라서 같은 Transfer를 51번 만들지 않고
         * Player를 각각 다르게 만든다.
         */
        IntStream.rangeClosed(1, 51)
                .forEach(i -> {

                    Player player =
                            savePlayer(
                                    String.format("Player%02d", i),
                                    (long) (1000 + i)
                            );

                    saveTransfer(
                            player,
                            chelsea,
                            arsenal,
                            "Transfer",
                            LocalDate.of(2026, 7, 1)
                    );
                });

        // when
        AllTransfersResponseDto firstPage =
                transferService.getTeamTransfers(
                        arsenal.getId(),
                        0,
                        "Player"
                );

        AllTransfersResponseDto secondPage =
                transferService.getTeamTransfers(
                        arsenal.getId(),
                        1,
                        "Player"
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
    @DisplayName("페이지 번호가 음수이면 팀 이적 정보를 조회할 수 없다.")
    void getTeamTransfers_negativePage_fail() {

        // given
        Long teamId = 1L;
        int page = -1;

        // when & then
        assertThatThrownBy(() ->
                transferService.getTeamTransfers(
                        teamId,
                        page,
                        ""
                )
        )
                .isInstanceOf(
                        InvalidTransferSearchPageValueException.class
                )
                .hasMessage(
                        "페이지 번호는 0 이상이어야 합니다."
                );
    }

    @Test
    @DisplayName("teamId 가 null 인 경우 예외가 발샐한다.")
    void getTeamTransfers_teamId_null_fail() {

        // given
        Long teamId = null;
        int page = 1;

        // when & then
        assertThatThrownBy(() ->
                transferService.getTeamTransfers(
                        teamId,
                        page,
                        ""
                )
        )
                .isInstanceOf(
                        InvalidTeamIdException.class
                )
                .hasMessage(
                        "잘못된 TeamId 값입니다."
                );
    }

    @Test
    @DisplayName("존재하지 않는 팀의 이적정보를 조회 시 예외가 발생한다.")
    void getTeamTransfers_not_found_team_fail() {

        // given
        Long teamId = 99999123L;
        int page = 1;

        given(teamService.getTeam(teamId))
                .willThrow(
                        new NotFoundTeamException()
                );

        // when & then
        assertThatThrownBy(() ->
                transferService.getTeamTransfers(
                        teamId,
                        page,
                        ""
                )
        )
                .isInstanceOf(
                        NotFoundTeamException.class
                )
                .hasMessage(
                        "존재하지 않는 팀 입니다."
                );
    }


    // ==================================================
    // 경계 상황
    // ==================================================

    @Test
    @DisplayName("검색어 앞뒤의 공백을 제거하고 팀 이적 정보를 검색한다.")
    void getTeamTransfers_trimKeyword() {

        // given
        Team arsenal = saveTeam("Arsenal", 1L);
        Team chelsea = saveTeam("Chelsea", 2L);

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
                transferService.getTeamTransfers(
                        arsenal.getId(),
                        0,
                        "   Saka   "
                );

        // then
        assertThat(response.getTransfers())
                .extracting(TransferResponseDto::getPlayerName)
                .containsExactly("Bukayo Saka");
    }


    @Test
    @DisplayName("검색 조건에 맞는 팀 이적 정보가 없으면 빈 리스트를 반환한다.")
    void getTeamTransfers_notFound() {

        // given
        Team arsenal = saveTeam("Arsenal", 1L);
        Team chelsea = saveTeam("Chelsea", 2L);

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
                transferService.getTeamTransfers(
                        arsenal.getId(),
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
    @DisplayName("검색어가 공백이면 해당 팀의 모든 이적 정보를 조회한다.")
    void getTeamTransfers_blankKeyword_findAll() {

        // given
        Team arsenal = saveTeam("Arsenal", 1L);
        Team chelsea = saveTeam("Chelsea", 2L);

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
                transferService.getTeamTransfers(
                        arsenal.getId(),
                        0,
                        "   "
                );

        // then
        assertThat(response.getTransfers())
                .hasSize(2);
    }


    @Test
    @DisplayName("검색어가 null이면 해당 팀의 모든 이적 정보를 조회한다.")
    void getTeamTransfers_nullKeyword_findAll() {

        // given
        Team arsenal = saveTeam("Arsenal", 1L);
        Team chelsea = saveTeam("Chelsea", 2L);

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
                transferService.getTeamTransfers(
                        arsenal.getId(),
                        0,
                        null
                );

        // then
        assertThat(response.getTransfers())
                .hasSize(1);
    }


    @Test
    @DisplayName("팀 이적 정보가 정확히 50개이면 다음 페이지가 존재하지 않는다.")
    void getTeamTransfers_exactPageSize() {

        // given
        Team arsenal = saveTeam("Arsenal", 1L);
        Team chelsea = saveTeam("Chelsea", 2L);

        IntStream.rangeClosed(1, 50)
                .forEach(i -> {

                    Player player =
                            savePlayer(
                                    String.format("Player%02d", i),
                                    (long) (1000 + i)
                            );

                    saveTransfer(
                            player,
                            chelsea,
                            arsenal,
                            "Transfer",
                            LocalDate.of(2026, 7, 1)
                    );
                });

        // when
        AllTransfersResponseDto response =
                transferService.getTeamTransfers(
                        arsenal.getId(),
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
    void getTeamTransfers_pageOutOfRange() {

        // given
        Team arsenal = saveTeam("Arsenal", 1L);
        Team chelsea = saveTeam("Chelsea", 2L);

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
                transferService.getTeamTransfers(
                        arsenal.getId(),
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