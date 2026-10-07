package com.dhoon.transfertracker.internal.transfer.service.impl;

import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.exception.InvalidTeamIdException;
import com.dhoon.transfertracker.internal.team.exception.NotFoundTeamException;
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

/*
 * 실제 요청 구조:
 *
 * Client
 *      ↓
 * Controller
 *      ↓
 * TransferOrchestrationService
 *      → Transaction X
 *      → page validation
 *      → teamId validation
 *      → keyword normalization
 *      → Pageable 생성
 *      ↓
 * TransferTxService
 *      → @Transactional(readOnly = true)
 *      → Team 존재 확인
 *      → Transfer 조회
 *      → Lazy 연관 Entity 접근
 *      → DTO 변환
 *      ↓
 * Transaction 종료
 *      ↓
 * DTO 반환
 *
 *
 * 실제 Transaction 경계를 검증하기 위해
 * Test Transaction은 사용하지 않는다.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TeamTransfersTest {

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
     * FK 자식부터 직접 삭제한다.
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
    @DisplayName("해당 팀으로 들어온 선수의 이적 정보를 조회할 수 있다.")
    void getTeamTransfers_inTeam() {

        // given
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

        Player saka =
                savePlayer(
                        "Bukayo Saka",
                        10L
                );


        // Chelsea -> Arsenal
        saveTransfer(
                saka,
                arsenal,
                chelsea,
                "Transfer",
                LocalDate.of(2026, 7, 1)
        );


        // when
        AllTransfersResponseDto response =
                transferOrchestrationService.getTeamTransfers(
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


    @Test
    @DisplayName("해당 팀에서 나간 선수의 이적 정보를 조회할 수 있다.")
    void getTeamTransfers_outTeam() {

        // given
        Team arsenal =
                saveTeam(
                        "Arsenal",
                        1L
                );

        Team barcelona =
                saveTeam(
                        "Barcelona",
                        2L
                );

        Player saka =
                savePlayer(
                        "Bukayo Saka",
                        10L
                );


        // Arsenal -> Barcelona
        saveTransfer(
                saka,
                barcelona,
                arsenal,
                "Transfer",
                LocalDate.of(2026, 7, 1)
        );


        // when
        AllTransfersResponseDto response =
                transferOrchestrationService.getTeamTransfers(
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
                .isEqualTo(
                        "Arsenal"
                );

        assertThat(transfer.getInTeamName())
                .isEqualTo(
                        "Barcelona"
                );
    }


    /*
     * Team 기준 조회는
     *
     * inTeam.id = teamId
     * OR
     * outTeam.id = teamId
     *
     * 를 모두 포함해야 한다.
     */
    @Test
    @DisplayName("팀의 영입과 방출 이적을 모두 조회한다.")
    void getTeamTransfers_inAndOut() {

        // given
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


        // Chelsea -> Arsenal
        saveTransfer(
                saka,
                arsenal,
                chelsea,
                "Transfer",
                LocalDate.of(2026, 7, 1)
        );

        // Arsenal -> Barcelona
        saveTransfer(
                son,
                barcelona,
                arsenal,
                "Transfer",
                LocalDate.of(2026, 6, 1)
        );


        // when
        AllTransfersResponseDto response =
                transferOrchestrationService.getTeamTransfers(
                        arsenal.getId(),
                        0,
                        ""
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
    @DisplayName("해당 팀과 관계없는 이적 정보는 조회하지 않는다.")
    void getTeamTransfers_excludeOtherTeams() {

        // given
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


        // Arsenal 관련
        saveTransfer(
                saka,
                arsenal,
                chelsea,
                "Arsenal Transfer",
                LocalDate.of(2026, 7, 1)
        );

        // Arsenal과 관계 없음
        saveTransfer(
                son,
                barcelona,
                chelsea,
                "Other Transfer",
                LocalDate.of(2026, 8, 1)
        );


        // when
        AllTransfersResponseDto response =
                transferOrchestrationService.getTeamTransfers(
                        arsenal.getId(),
                        0,
                        ""
                );


        // then
        assertThat(response.getTransfers())
                .hasSize(1);

        assertThat(response.getTransfers())
                .extracting(
                        TransferResponseDto::getType
                )
                .containsExactly(
                        "Arsenal Transfer"
                );
    }


    /*
     * Transfer.player / inTeam / outTeam 모두 LAZY.
     *
     * Test Transaction이 없기 때문에
     * TxService Transaction 안에서 DTO 변환이 끝나야 한다.
     */
    @Test
    @DisplayName("팀 이적 정보의 연관 Entity는 Transaction 내부에서 DTO로 변환된다.")
    void getTeamTransfers_dtoMapping() {

        // given
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

        Player saka =
                savePlayer(
                        "Bukayo Saka",
                        10L
                );

        saveTransfer(
                saka,
                arsenal,
                chelsea,
                "Permanent",
                LocalDate.of(2026, 7, 1)
        );


        // when
        AllTransfersResponseDto response =
                transferOrchestrationService.getTeamTransfers(
                        arsenal.getId(),
                        0,
                        ""
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

        assertThat(transfer.getPhotoUrl())
                .isEqualTo(
                        "https://media.api-sports.io/football/players/10.png"
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
    }


    // ==================================================
    // Keyword 검색
    // ==================================================

    @Test
    @DisplayName("팀 이적 정보에서 선수 이름 일부로 검색할 수 있다.")
    void getTeamTransfers_searchByPlayerName() {

        // given
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
                transferOrchestrationService.getTeamTransfers(
                        arsenal.getId(),
                        0,
                        "Saka"
                );


        // then

        assertThat(response.getTransfers()).hasSize(1);

        assertThat(response.getTransfers())
                .extracting(
                        TransferResponseDto::getPlayerName
                )
                .containsExactly(
                        "Bukayo Saka"
                );
    }


    @Test
    @DisplayName("팀 이적 검색은 선수 이름의 대소문자를 구분하지 않는다.")
    void getTeamTransfers_ignoreCase() {

        // given
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
                transferOrchestrationService.getTeamTransfers(
                        arsenal.getId(),
                        0,
                        "sAkA"
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
    @DisplayName("검색어 앞뒤 공백을 제거하고 팀 이적 정보를 검색한다.")
    void getTeamTransfers_trimKeyword() {

        // given
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
                transferOrchestrationService.getTeamTransfers(
                        arsenal.getId(),
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


    // ==================================================
    // 정렬
    // ==================================================

    @Test
    @DisplayName("팀 이적 정보는 이적 날짜 내림차순으로 조회된다.")
    void getTeamTransfers_sortByTransferDateDesc() {

        // given
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

        Player bruno =
                savePlayer(
                        "Bruno Fernandes",
                        30L
                );


        saveTransfer(
                saka,
                arsenal,
                chelsea,
                "Transfer",
                LocalDate.of(2024, 7, 1)
        );

        saveTransfer(
                son,
                barcelona,
                arsenal,
                "Transfer",
                LocalDate.of(2026, 7, 1)
        );

        saveTransfer(
                bruno,
                arsenal,
                chelsea,
                "Transfer",
                LocalDate.of(2025, 7, 1)
        );


        // when
        AllTransfersResponseDto response =
                transferOrchestrationService.getTeamTransfers(
                        arsenal.getId(),
                        0,
                        ""
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
    void getTeamTransfers_sameDate_sortByIdDesc() {

        // given
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

        LocalDate sameDate =
                LocalDate.of(2026, 7, 1);


        saveTransfer(
                saka,
                arsenal,
                chelsea,
                "First Transfer",
                sameDate
        );

        saveTransfer(
                son,
                chelsea,
                arsenal,
                "Second Transfer",
                sameDate
        );


        // when
        AllTransfersResponseDto response =
                transferOrchestrationService.getTeamTransfers(
                        arsenal.getId(),
                        0,
                        ""
                );


        // then
        assertThat(response.getTransfers())
                .extracting(
                        TransferResponseDto::getType
                )
                .containsExactly(
                        "Second Transfer",
                        "First Transfer"
                );
    }


    // ==================================================
    // Paging
    // ==================================================

    @Test
    @DisplayName("팀 이적 검색 결과가 50개를 초과하면 다음 페이지가 존재한다.")
    void getTeamTransfers_paging() {

        // given
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
                transferOrchestrationService.getTeamTransfers(
                        arsenal.getId(),
                        0,
                        "Player"
                );

        AllTransfersResponseDto secondPage =
                transferOrchestrationService.getTeamTransfers(
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


    @Test
    @DisplayName("팀 이적 정보가 정확히 50개이면 다음 페이지가 존재하지 않는다.")
    void getTeamTransfers_exactPageSize() {

        // given
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
                transferOrchestrationService.getTeamTransfers(
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
    @DisplayName("존재하지 않는 페이지는 빈 리스트를 반환한다.")
    void getTeamTransfers_pageOutOfRange() {

        // given
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
                transferOrchestrationService.getTeamTransfers(
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
    // 경계 상황
    // ==================================================

    @Test
    @DisplayName("팀은 존재하지만 이적 기록이 없으면 빈 리스트를 반환한다.")
    void getTeamTransfers_empty() {

        // given
        Team arsenal =
                saveTeam(
                        "Arsenal",
                        1L
                );


        // when
        AllTransfersResponseDto response =
                transferOrchestrationService.getTeamTransfers(
                        arsenal.getId(),
                        0,
                        ""
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
    @DisplayName("검색어가 null이면 해당 팀의 전체 이적 정보를 조회한다.")
    void getTeamTransfers_nullKeyword_findAll() {

        // given
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
                transferOrchestrationService.getTeamTransfers(
                        arsenal.getId(),
                        0,
                        null
                );


        // then
        assertThat(response.getTransfers())
                .hasSize(1);
    }


    // ==================================================
    // 실패 상황
    // ==================================================

    /*
     * page는 DB 접근 필요 없음.
     *
     * Orchestration에서 차단된다.
     */
    @Test
    @DisplayName("페이지 번호가 음수이면 예외가 발생한다.")
    void getTeamTransfers_negativePage() {

        // when & then
        assertThatThrownBy(() ->
                transferOrchestrationService.getTeamTransfers(
                        1L,
                        -1,
                        ""
                )
        )
                .isInstanceOf(
                        InvalidTransferSearchPageValueException.class
                );
    }


    /*
     * null teamId 역시
     * Orchestration에서 차단된다.
     */
    @Test
    @DisplayName("teamId가 null이면 예외가 발생한다.")
    void getTeamTransfers_nullTeamId() {

        // when & then
        assertThatThrownBy(() ->
                transferOrchestrationService.getTeamTransfers(
                        null,
                        0,
                        ""
                )
        )
                .isInstanceOf(
                        InvalidTeamIdException.class
                );
    }


    /*
     * teamId 자체는 유효한 형태지만
     * DB에 Team이 없다.
     *
     * TxService에서 TeamRepository를 통해 판단한다.
     */
    @Test
    @DisplayName("존재하지 않는 팀의 이적 정보를 조회하면 예외가 발생한다.")
    void getTeamTransfers_teamNotFound() {

        // given
        Long teamId = 999999L;


        // when & then
        assertThatThrownBy(() ->
                transferOrchestrationService.getTeamTransfers(
                        teamId,
                        0,
                        ""
                )
        )
                .isInstanceOf(
                        NotFoundTeamException.class
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


    /*
     * Transfer.of() 순서와 맞춘다.
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