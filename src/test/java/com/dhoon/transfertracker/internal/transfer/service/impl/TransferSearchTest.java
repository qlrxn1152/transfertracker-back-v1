package com.dhoon.transfertracker.internal.transfer.service.impl;

import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.transfer.domain.Transfer;
import com.dhoon.transfertracker.internal.transfer.dto.response.AllTransfersResponseDto;
import com.dhoon.transfertracker.internal.transfer.dto.response.TransferResponseDto;
import com.dhoon.transfertracker.internal.transfer.exception.InvalidTransferSearchPageValueException;
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
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@DataJpaTest
@Import({
        TransferOrchestrationService.class,
        TransferTxService.class
})
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TransferSearchTest {

    @Autowired
    TransferOrchestrationService transferOrchestrationService;

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
    // 기본 검색
    // ==================================================

    @Test
    @DisplayName("선수 이름의 일부를 이용해서 이적 정보를 검색할 수 있다.")
    void searchTransfer_byPartialPlayerName() {

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
                arsenal,
                chelsea,
                "Transfer",
                LocalDate.of(2026, 7, 1)
        );

        saveTransfer(
                son,
                chelsea,
                arsenal,
                "Transfer",
                LocalDate.of(2026, 6, 1)
        );


        // when
        AllTransfersResponseDto response =
                transferOrchestrationService.getTransfers(
                        0,
                        "Saka",
                        null,
                        null
                );


        // then
        assertThat(response.getTransfers())
                .hasSize(1);

        assertThat(response.getTransfers())
                .extracting(
                        TransferResponseDto::getPlayerName
                )
                .containsExactly(
                        "Bukayo Saka"
                );
    }


    @Test
    @DisplayName("선수 이름 검색은 대소문자를 구분하지 않는다.")
    void searchTransfer_ignoreCase() {

        // given
        Team arsenal =
                saveTeam("Arsenal", 1L);

        Team chelsea =
                saveTeam("Chelsea", 2L);

        Player saka =
                savePlayer("Bukayo Saka", 10L);

        saveTransfer(
                saka,
                arsenal,
                chelsea,
                "Transfer",
                LocalDate.of(2026, 7, 1)
        );


        // when
        AllTransfersResponseDto response =
                transferOrchestrationService.getTransfers(
                        0,
                        "sAkA",
                        null,
                        null
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


    /*
     * Transfer.player / inTeam / outTeam은 모두 LAZY.
     *
     * 실제 Test Transaction이 없으므로
     * TransferTxService 안에서 DTO 변환이 완료되어야 한다.
     */
    @Test
    @DisplayName("이적 정보의 연관 Entity는 Transaction 내부에서 DTO로 변환된다.")
    void searchTransfer_mappingAssociatedEntities() {

        // given
        Team arsenal =
                saveTeam("Arsenal", 1L);

        Team chelsea =
                saveTeam("Chelsea", 2L);

        Player saka =
                savePlayer("Bukayo Saka", 10L);

        saveTransfer(
                saka,
                arsenal,
                chelsea,
                "Permanent",
                LocalDate.of(2026, 7, 1)
        );


        // when
        AllTransfersResponseDto response =
                transferOrchestrationService.getTransfers(
                        0,
                        "Saka",
                        null,
                        null
                );


        // then
        TransferResponseDto transfer =
                response.getTransfers().get(0);

        assertThat(transfer.getPlayerId())
                .isEqualTo(
                        saka.getId()
                );

        assertThat(transfer.getPlayerName())
                .isEqualTo(
                        "Bukayo Saka"
                );

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
                        "Permanent"
                );

        assertThat(transfer.getDate())
                .isEqualTo(
                        LocalDate.of(2026, 7, 1)
                );

        assertThat(transfer.getPhotoUrl())
                .isEqualTo(
                        "https://media.api-sports.io/football/players/10.png"
                );
    }


    // ==================================================
    // 정렬
    // ==================================================

    @Test
    @DisplayName("이적 정보는 이적 날짜 내림차순으로 조회된다.")
    void searchTransfer_sortByTransferDateDesc() {

        // given
        Team arsenal =
                saveTeam("Arsenal", 1L);

        Team chelsea =
                saveTeam("Chelsea", 2L);

        Player saka =
                savePlayer("Bukayo Saka", 10L);

        Player son =
                savePlayer("Son Heung-min", 20L);

        Player bruno =
                savePlayer("Bruno Fernandes", 30L);


        saveTransfer(
                saka,
                arsenal,
                chelsea,
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
                arsenal,
                chelsea,
                "Transfer",
                LocalDate.of(2026, 2, 1)
        );


        // when
        AllTransfersResponseDto response =
                transferOrchestrationService.getTransfers(
                        0,
                        "",
                        null,
                        null
                );


        // then
        assertThat(response.getTransfers())
                .extracting(
                        TransferResponseDto::getPlayerName
                )
                .containsExactly(
                        "Son Heung-min",
                        "Bruno Fernandes",
                        "Bukayo Saka"
                );
    }


    @Test
    @DisplayName("이적 날짜가 같으면 ID 내림차순으로 조회된다.")
    void searchTransfer_sameDate_sortByIdDesc() {

        // given
        Team arsenal =
                saveTeam("Arsenal", 1L);

        Team chelsea =
                saveTeam("Chelsea", 2L);

        Player saka =
                savePlayer("Bukayo Saka", 10L);

        Player son =
                savePlayer("Son Heung-min", 20L);

        LocalDate sameDate =
                LocalDate.of(2026, 7, 1);


        // 먼저 저장
        saveTransfer(
                saka,
                arsenal,
                chelsea,
                "First",
                sameDate
        );

        // 나중 저장
        saveTransfer(
                son,
                arsenal,
                chelsea,
                "Second",
                sameDate
        );


        // when
        AllTransfersResponseDto response =
                transferOrchestrationService.getTransfers(
                        0,
                        "",
                        null,
                        null
                );


        // then
        assertThat(response.getTransfers())
                .extracting(
                        TransferResponseDto::getPlayerName
                )
                .containsExactly(
                        "Son Heung-min",
                        "Bukayo Saka"
                );
    }


    // ==================================================
    // League 필터
    // ==================================================

    @Test
    @DisplayName("inTeam이 선택한 리그에 속하면 이적 정보를 조회한다.")
    void searchTransfer_filterByLeague_inTeam() {

        // given
        Team arsenal =
                saveTeam(
                        "Arsenal",
                        1L,
                        LeagueCode.EPL
                );

        Team barcelona =
                saveTeam(
                        "Barcelona",
                        2L,
                        LeagueCode.LA_LIGA
                );

        Player player =
                savePlayer(
                        "Test Player",
                        10L
                );

        saveTransfer(
                player,
                arsenal,
                barcelona,
                "Transfer",
                LocalDate.of(2026, 7, 1)
        );


        // when
        AllTransfersResponseDto response =
                transferOrchestrationService.getTransfers(
                        0,
                        "",
                        LeagueCode.EPL,
                        null
                );


        // then
        assertThat(response.getTransfers())
                .hasSize(1);

        assertThat(response.getTransfers().get(0).getInTeamName())
                .isEqualTo(
                        "Arsenal"
                );
    }


    @Test
    @DisplayName("outTeam이 선택한 리그에 속하면 이적 정보를 조회한다.")
    void searchTransfer_filterByLeague_outTeam() {

        // given
        Team arsenal =
                saveTeam(
                        "Arsenal",
                        1L,
                        LeagueCode.EPL
                );

        Team barcelona =
                saveTeam(
                        "Barcelona",
                        2L,
                        LeagueCode.LA_LIGA
                );

        Player player =
                savePlayer(
                        "Test Player",
                        10L
                );

        saveTransfer(
                player,
                barcelona,
                arsenal,
                "Transfer",
                LocalDate.of(2026, 7, 1)
        );


        // when
        AllTransfersResponseDto response =
                transferOrchestrationService.getTransfers(
                        0,
                        "",
                        LeagueCode.EPL,
                        null
                );


        // then
        assertThat(response.getTransfers())
                .hasSize(1);

        assertThat(response.getTransfers().get(0).getOutTeamName())
                .isEqualTo(
                        "Arsenal"
                );
    }


    @Test
    @DisplayName("선택한 리그와 관련 없는 이적 정보는 조회하지 않는다.")
    void searchTransfer_filterByLeague_excludeOtherLeague() {

        // given
        Team barcelona =
                saveTeam(
                        "Barcelona",
                        1L,
                        LeagueCode.LA_LIGA
                );

        Team realMadrid =
                saveTeam(
                        "Real Madrid",
                        2L,
                        LeagueCode.LA_LIGA
                );

        Player player =
                savePlayer(
                        "Test Player",
                        10L
                );

        saveTransfer(
                player,
                barcelona,
                realMadrid,
                "Transfer",
                LocalDate.of(2026, 7, 1)
        );


        // when
        AllTransfersResponseDto response =
                transferOrchestrationService.getTransfers(
                        0,
                        "",
                        LeagueCode.EPL,
                        null
                );


        // then
        assertThat(response.getTransfers())
                .isEmpty();
    }


    // ==================================================
    // Team 필터
    // ==================================================

    @Test
    @DisplayName("inTeam 또는 outTeam이 선택한 팀이면 조회한다.")
    void searchTransfer_filterByTeam() {

        // given
        Team arsenal =
                saveTeam(
                        "Arsenal",
                        1L,
                        LeagueCode.EPL
                );

        Team chelsea =
                saveTeam(
                        "Chelsea",
                        2L,
                        LeagueCode.EPL
                );

        Team barcelona =
                saveTeam(
                        "Barcelona",
                        3L,
                        LeagueCode.LA_LIGA
                );

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


        // Arsenal → Chelsea
        saveTransfer(
                saka,
                arsenal,
                chelsea,
                "Transfer",
                LocalDate.of(2026, 7, 1)
        );

        // Chelsea → Barcelona
        saveTransfer(
                son,
                chelsea,
                barcelona,
                "Transfer",
                LocalDate.of(2026, 6, 1)
        );


        // when
        AllTransfersResponseDto response =
                transferOrchestrationService.getTransfers(
                        0,
                        "",
                        null,
                        arsenal.getId()
                );


        // then
        assertThat(response.getTransfers())
                .hasSize(1);

        assertThat(response.getTransfers())
                .extracting(
                        TransferResponseDto::getPlayerName
                )
                .containsExactly(
                        "Bukayo Saka"
                );
    }


    /*
     * 여기 중요.
     *
     * getTransfers()의 teamId는 조회 대상 Resource가 아니라
     * optional filter다.
     *
     * 따라서 존재하지 않는 ID라도 예외가 아니다.
     */
    @Test
    @DisplayName("존재하지 않는 팀 ID로 필터링하면 빈 리스트를 반환한다.")
    void searchTransfer_nonexistentTeam_findEmpty() {

        // given
        Team arsenal =
                saveTeam(
                        "Arsenal",
                        1L,
                        LeagueCode.EPL
                );

        Team chelsea =
                saveTeam(
                        "Chelsea",
                        2L,
                        LeagueCode.EPL
                );

        Player saka =
                savePlayer(
                        "Bukayo Saka",
                        10L
                );

        saveTransfer(
                saka,
                arsenal,
                chelsea,
                "Transfer",
                LocalDate.of(2026, 7, 1)
        );


        // when
        AllTransfersResponseDto response =
                transferOrchestrationService.getTransfers(
                        0,
                        "",
                        null,
                        999999L
                );


        // then
        assertThat(response.getTransfers())
                .isEmpty();

        assertThat(response.isHasNext())
                .isFalse();

        assertThat(response.isHasPrevious())
                .isFalse();
    }


    // ==================================================
    // 복합 필터
    // ==================================================

    @Test
    @DisplayName("리그, 팀, 선수 이름을 모두 만족하는 이적 정보만 조회한다.")
    void searchTransfer_filterByLeagueAndTeamAndKeyword() {

        // given
        Team arsenal =
                saveTeam(
                        "Arsenal",
                        1L,
                        LeagueCode.EPL
                );

        Team chelsea =
                saveTeam(
                        "Chelsea",
                        2L,
                        LeagueCode.EPL
                );

        Team barcelona =
                saveTeam(
                        "Barcelona",
                        3L,
                        LeagueCode.LA_LIGA
                );

        Team realMadrid =
                saveTeam(
                        "Real Madrid",
                        4L,
                        LeagueCode.LA_LIGA
                );


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


        // 모든 조건 만족
        saveTransfer(
                saka,
                arsenal,
                chelsea,
                "Transfer",
                LocalDate.of(2026, 7, 1)
        );

        // league/team O, keyword X
        saveTransfer(
                son,
                arsenal,
                chelsea,
                "Transfer",
                LocalDate.of(2026, 6, 1)
        );

        // keyword O, league/team X
        saveTransfer(
                saka,
                barcelona,
                realMadrid,
                "Loan",
                LocalDate.of(2025, 7, 1)
        );


        // when
        AllTransfersResponseDto response =
                transferOrchestrationService.getTransfers(
                        0,
                        "Saka",
                        LeagueCode.EPL,
                        arsenal.getId()
                );


        // then
        assertThat(response.getTransfers())
                .hasSize(1);

        TransferResponseDto transfer =
                response.getTransfers().get(0);

        assertThat(transfer.getPlayerName())
                .isEqualTo(
                        "Bukayo Saka"
                );

        assertThat(transfer.getInTeamName())
                .isEqualTo(
                        "Arsenal"
                );

        assertThat(transfer.getOutTeamName())
                .isEqualTo(
                        "Chelsea"
                );
    }


    // ==================================================
    // Paging
    // ==================================================

    @Test
    @DisplayName("검색 결과가 50개를 초과하면 Slice 페이징 정보가 정상적으로 반환된다.")
    void searchTransfer_paging() {

        // given
        Team arsenal =
                saveTeam(
                        "Arsenal",
                        100L
                );

        Team chelsea =
                saveTeam(
                        "Chelsea",
                        200L
                );


        IntStream.rangeClosed(1, 51)
                .forEach(i -> {

                    Player player =
                            savePlayer(
                                    String.format(
                                            "Player%02d",
                                            i
                                    ),
                                    1000L + i
                            );

                    saveTransfer(
                            player,
                            arsenal,
                            chelsea,
                            "Transfer",
                            LocalDate.of(2026, 7, 1)
                    );
                });


        // when
        AllTransfersResponseDto firstPage =
                transferOrchestrationService.getTransfers(
                        0,
                        "Player",
                        null,
                        null
                );

        AllTransfersResponseDto secondPage =
                transferOrchestrationService.getTransfers(
                        1,
                        "Player",
                        null,
                        null
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


    @Test
    @DisplayName("검색 결과가 정확히 50개이면 다음 페이지가 존재하지 않는다.")
    void searchTransfer_exactPageSize() {

        // given
        Team arsenal =
                saveTeam(
                        "Arsenal",
                        100L
                );

        Team chelsea =
                saveTeam(
                        "Chelsea",
                        200L
                );


        IntStream.rangeClosed(1, 50)
                .forEach(i -> {

                    Player player =
                            savePlayer(
                                    String.format(
                                            "Player%02d",
                                            i
                                    ),
                                    1000L + i
                            );

                    saveTransfer(
                            player,
                            arsenal,
                            chelsea,
                            "Transfer",
                            LocalDate.of(2026, 7, 1)
                    );
                });


        // when
        AllTransfersResponseDto response =
                transferOrchestrationService.getTransfers(
                        0,
                        "Player",
                        null,
                        null
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
                arsenal,
                chelsea,
                "Transfer",
                LocalDate.of(2026, 7, 1)
        );


        // when
        AllTransfersResponseDto response =
                transferOrchestrationService.getTransfers(
                        100,
                        "",
                        null,
                        null
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
    // Keyword / 필터 없음
    // ==================================================

    @Test
    @DisplayName("검색어 앞뒤 공백을 제거하고 검색한다.")
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
                arsenal,
                chelsea,
                "Transfer",
                LocalDate.of(2026, 7, 1)
        );


        // when
        AllTransfersResponseDto response =
                transferOrchestrationService.getTransfers(
                        0,
                        "   Saka   ",
                        null,
                        null
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


    @Test
    @DisplayName("검색어와 필터가 null이면 모든 이적 정보를 조회한다.")
    void searchTransfer_nullFilters_findAll() {

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
                arsenal,
                chelsea,
                "Transfer",
                LocalDate.of(2026, 7, 1)
        );

        saveTransfer(
                son,
                chelsea,
                arsenal,
                "Transfer",
                LocalDate.of(2026, 6, 1)
        );


        // when
        AllTransfersResponseDto response =
                transferOrchestrationService.getTransfers(
                        0,
                        null,
                        null,
                        null
                );


        // then
        assertThat(response.getTransfers())
                .hasSize(2);

        assertThat(response.getTransfers())
                .extracting(
                        TransferResponseDto::getPlayerName
                )
                .containsExactly(
                        "Bukayo Saka",
                        "Son Heung-min"
                );
    }


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
                arsenal,
                chelsea,
                "Transfer",
                LocalDate.of(2026, 7, 1)
        );


        // when
        AllTransfersResponseDto response =
                transferOrchestrationService.getTransfers(
                        0,
                        "Messi",
                        null,
                        null
                );


        // then
        assertThat(response.getTransfers())
                .isEmpty();

        assertThat(response.isHasNext())
                .isFalse();

        assertThat(response.isHasPrevious())
                .isFalse();
    }


    // ==================================================
    // 실패 상황
    // ==================================================

    @Test
    @DisplayName("페이지 번호가 음수이면 예외가 발생한다.")
    void searchTransfer_negativePage() {

        // when & then
        assertThatThrownBy(() ->
                transferOrchestrationService.getTransfers(
                        -1,
                        "",
                        null,
                        null
                )
        )
                .isInstanceOf(
                        InvalidTransferSearchPageValueException.class
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


    private Team saveTeam(
            String teamName,
            Long apiFootballId,
            LeagueCode leagueCode
    ) {

        return teamRepository.save(
                Team.of(
                        teamName,
                        apiFootballId,
                        leagueCode
                )
        );
    }


    /*
     * 파라미터 순서를 Entity.of()와 동일하게 맞춘다.
     *
     * Player
     * inTeam
     * outTeam
     */
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