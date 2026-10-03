package com.dhoon.transfertracker.internal.transfer.service.impl;

import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.player.service.PlayerService;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
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
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@DataJpaTest
@Import(TransferServiceImpl.class)
@ActiveProfiles("test")
class TransferSearchTest {

    @Autowired
    TransferService transferService;

    @Autowired
    TransferRepository transferRepository;

    @Autowired
    PlayerRepository playerRepository;

    @Autowired
    TeamRepository teamRepository;

    @MockitoBean
    PlayerService playerService;

    @MockitoBean
    TeamService teamService;


    // ==================================================
    // 정상 상황
    // ==================================================

    // [기존 수정]
    // 기존 검색 테스트 그대로.
    // getTransfers()에 leagueCode 파라미터가 추가됐기 때문에
    // null을 전달해서 "리그 필터 없음"이라는 기존 동작을 검증한다.
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
                        "Saka",
                        null
                );

        // then
        assertThat(response.getTransfers())
                .hasSize(1);

        assertThat(response.getTransfers())
                .extracting(TransferResponseDto::getPlayerName)
                .containsExactly("Bukayo Saka");
    }


    // [기존 수정]
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
                        "sAkA",
                        null
                );

        // then
        assertThat(response.getTransfers())
                .extracting(TransferResponseDto::getPlayerName)
                .containsExactly("Bukayo Saka");
    }


    // [기존 수정]
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
                        "Saka",
                        null
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


    // [기존 수정]
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
                        "Saka",
                        null
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


    // [기존 수정]
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
                        "",
                        null
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


    // [기존 수정]
    // 날짜가 같다면 PK 내림차순이므로
    // 나중에 저장된 이적 정보가 먼저 조회된다.
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

        saveTransfer(
                saka,
                chelsea,
                arsenal,
                "Transfer",
                sameDate
        );

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
                        "",
                        null
                );

        // then
        assertThat(response.getTransfers())
                .extracting(TransferResponseDto::getPlayerName)
                .containsExactly(
                        "Son Heung-min",
                        "Bukayo Saka"
                );
    }


    // [기존 수정]
    @Test
    @DisplayName("검색 결과가 50개를 초과하면 다음 페이지가 존재한다.")
    void searchTransfer_paging() {

        // given
        IntStream.rangeClosed(1, 51)
                .forEach(i -> {

                    Player player =
                            savePlayer(
                                    "Test Player" + i,
                                    Long.valueOf(i)
                            );

                    Team inTeam =
                            saveTeam(
                                    "inteam" + i,
                                    Long.valueOf(i + 100)
                            );

                    Team outTeam =
                            saveTeam(
                                    "outteam" + i,
                                    Long.valueOf(i + 200)
                            );

                    saveTransfer(
                            player,
                            outTeam,
                            inTeam,
                            "transfer",
                            LocalDate.of(2026, 2, 3)
                    );
                });

        // when
        AllTransfersResponseDto firstPage =
                transferService.getTransfers(
                        0,
                        "Test",
                        null
                );

        AllTransfersResponseDto secondPage =
                transferService.getTransfers(
                        1,
                        "Test",
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


    // ==================================================
    // [신규] 정상 상황 - 리그 필터
    // ==================================================

    /*
     * [신규]
     *
     * Repository JPQL:
     *
     * it.leagueCode = :leagueCode
     * OR
     * ot.leagueCode = :leagueCode
     *
     * 중 첫 번째 조건을 검증한다.
     */
    @Test
    @DisplayName("inTeam이 선택한 리그에 속하면 해당 이적 정보를 조회한다.")
    void searchTransfer_filterByLeague_inTeam() {

        // given
        Team barcelona = saveTeam(
                "Barcelona",
                1L,
                LeagueCode.LA_LIGA
        );

        Team manUnited = saveTeam(
                "Manchester United",
                2L,
                LeagueCode.EPL
        );

        Player player = savePlayer(
                "Test Player",
                10L
        );

        saveTransfer(
                player,
                barcelona,      // outTeam
                manUnited,      // inTeam
                "Transfer",
                LocalDate.of(2026, 7, 1)
        );

        // when
        AllTransfersResponseDto response =
                transferService.getTransfers(
                        0,
                        "",
                        LeagueCode.EPL
                );

        // then
        assertThat(response.getTransfers())
                .hasSize(1);

        assertThat(response.getTransfers())
                .extracting(TransferResponseDto::getPlayerName)
                .containsExactly("Test Player");

        assertThat(response.getTransfers())
                .extracting(TransferResponseDto::getOutTeamName)
                .containsExactly("Barcelona");

        assertThat(response.getTransfers())
                .extracting(TransferResponseDto::getInTeamName)
                .containsExactly("Manchester United");
    }


    /*
     * [신규]
     *
     * JPQL의 두 번째 리그 조건:
     *
     * ot.leagueCode = :leagueCode
     *
     * 를 독립적으로 검증한다.
     */
    @Test
    @DisplayName("outTeam이 선택한 리그에 속하면 해당 이적 정보를 조회한다.")
    void searchTransfer_filterByLeague_outTeam() {

        // given
        Team manUnited = saveTeam(
                "Manchester United",
                1L,
                LeagueCode.EPL
        );

        Team realMadrid = saveTeam(
                "Real Madrid",
                2L,
                LeagueCode.LA_LIGA
        );

        Player player = savePlayer(
                "Test Player",
                10L
        );

        saveTransfer(
                player,
                manUnited,      // outTeam
                realMadrid,     // inTeam
                "Transfer",
                LocalDate.of(2026, 7, 1)
        );

        // when
        AllTransfersResponseDto response =
                transferService.getTransfers(
                        0,
                        "",
                        LeagueCode.EPL
                );

        // then
        assertThat(response.getTransfers())
                .hasSize(1);

        assertThat(response.getTransfers())
                .extracting(TransferResponseDto::getPlayerName)
                .containsExactly("Test Player");

        assertThat(response.getTransfers())
                .extracting(TransferResponseDto::getOutTeamName)
                .containsExactly("Manchester United");

        assertThat(response.getTransfers())
                .extracting(TransferResponseDto::getInTeamName)
                .containsExactly("Real Madrid");
    }


    /*
     * [신규]
     *
     * EPL 필터인데
     * inTeam / outTeam 둘 다 LA_LIGA라면
     * Repository WHERE 조건이 false가 되어야 한다.
     */
    @Test
    @DisplayName("inTeam과 outTeam 모두 선택한 리그가 아니면 조회하지 않는다.")
    void searchTransfer_filterByLeague_excludeOtherLeague() {

        // given
        Team barcelona = saveTeam(
                "Barcelona",
                1L,
                LeagueCode.LA_LIGA
        );

        Team realMadrid = saveTeam(
                "Real Madrid",
                2L,
                LeagueCode.LA_LIGA
        );

        Player player = savePlayer(
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
                transferService.getTransfers(
                        0,
                        "",
                        LeagueCode.EPL
                );

        // then
        assertThat(response.getTransfers())
                .isEmpty();
    }


    /*
     * [신규]
     *
     * Arsenal -> Chelsea처럼
     * inTeam과 outTeam이 모두 EPL이어도
     * 같은 Transfer row는 1건만 조회되어야 한다.
     */
    @Test
    @DisplayName("inTeam과 outTeam 모두 선택한 리그여도 이적 정보는 중복 없이 한 건만 조회한다.")
    void searchTransfer_filterByLeague_sameLeague_noDuplicate() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                1L,
                LeagueCode.EPL
        );

        Team chelsea = saveTeam(
                "Chelsea",
                2L,
                LeagueCode.EPL
        );

        Player saka = savePlayer(
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
                transferService.getTransfers(
                        0,
                        "",
                        LeagueCode.EPL
                );

        // then
        assertThat(response.getTransfers())
                .hasSize(1);

        assertThat(response.getTransfers())
                .extracting(TransferResponseDto::getPlayerName)
                .containsExactly("Bukayo Saka");
    }


    /*
     * [신규]
     *
     * :leagueCode is null
     *
     * 이 true가 되면 리그 조건이 모든 row에 대해 통과하므로
     * EPL / LA_LIGA 모두 조회되는지 명시적으로 검증한다.
     *
     * 기존 테스트들도 모두 null을 전달하므로
     * regression test 역할을 하지만,
     * 현재 비즈니스 정책을 명확하게 남기기 위해 별도 테스트를 둔다.
     */
    @Test
    @DisplayName("리그 조건이 null이면 모든 리그의 이적 정보를 조회한다.")
    void searchTransfer_nullLeague_findAllLeague() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                1L,
                LeagueCode.EPL
        );

        Team chelsea = saveTeam(
                "Chelsea",
                2L,
                LeagueCode.EPL
        );

        Team barcelona = saveTeam(
                "Barcelona",
                3L,
                LeagueCode.LA_LIGA
        );

        Team realMadrid = saveTeam(
                "Real Madrid",
                4L,
                LeagueCode.LA_LIGA
        );

        Player saka = savePlayer(
                "Bukayo Saka",
                10L
        );

        Player pedri = savePlayer(
                "Pedri",
                20L
        );

        // EPL -> EPL
        saveTransfer(
                saka,
                arsenal,
                chelsea,
                "Transfer",
                LocalDate.of(2026, 7, 1)
        );

        // LALIGA -> LALIGA
        saveTransfer(
                pedri,
                barcelona,
                realMadrid,
                "Transfer",
                LocalDate.of(2026, 6, 1)
        );

        // when
        AllTransfersResponseDto response =
                transferService.getTransfers(
                        0,
                        "",
                        null
                );

        // then
        assertThat(response.getTransfers())
                .extracting(TransferResponseDto::getPlayerName)
                .containsExactly(
                        "Bukayo Saka",
                        "Pedri"
                );
    }


    /*
     * [신규 - 중요]
     *
     * 현재 JPQL 구조:
     *
     * (리그 조건)
     * AND
     * (keyword 조건)
     *
     * 둘 중 하나만 만족해서는 안 되고
     * 두 조건을 모두 만족해야 조회되어야 한다.
     */
    @Test
    @DisplayName("리그와 선수 이름 검색 조건을 모두 만족하는 이적 정보만 조회한다.")
    void searchTransfer_filterByLeagueAndKeyword() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                1L,
                LeagueCode.EPL
        );

        Team chelsea = saveTeam(
                "Chelsea",
                2L,
                LeagueCode.EPL
        );

        Team barcelona = saveTeam(
                "Barcelona",
                3L,
                LeagueCode.LA_LIGA
        );

        Team realMadrid = saveTeam(
                "Real Madrid",
                4L,
                LeagueCode.LA_LIGA
        );

        Player saka = savePlayer(
                "Bukayo Saka",
                10L
        );

        Player son = savePlayer(
                "Son Heung-min",
                20L
        );

        /*
         * Saka + EPL
         * → 둘 다 만족
         * → 조회 대상
         */
        saveTransfer(
                saka,
                arsenal,
                chelsea,
                "Transfer",
                LocalDate.of(2026, 7, 1)
        );

        /*
         * Son + EPL
         * → 리그는 맞지만 keyword가 다름
         * → 제외
         */
        saveTransfer(
                son,
                arsenal,
                chelsea,
                "Transfer",
                LocalDate.of(2026, 6, 1)
        );

        /*
         * Saka + LA_LIGA
         * → keyword는 맞지만 리그가 다름
         * → 제외
         */
        saveTransfer(
                saka,
                barcelona,
                realMadrid,
                "Loan",
                LocalDate.of(2025, 7, 1)
        );

        // when
        AllTransfersResponseDto response =
                transferService.getTransfers(
                        0,
                        "Saka",
                        LeagueCode.EPL
                );

        // then
        assertThat(response.getTransfers())
                .hasSize(1);

        TransferResponseDto transfer =
                response.getTransfers().get(0);

        assertThat(transfer.getPlayerName())
                .isEqualTo("Bukayo Saka");

        assertThat(transfer.getOutTeamName())
                .isEqualTo("Arsenal");

        assertThat(transfer.getInTeamName())
                .isEqualTo("Chelsea");
    }


    /*
     * [신규 - 중요]
     *
     * 리그 필터는 Java Stream에서 페이지 조회 후 처리하는 것이 아니라,
     * DB WHERE 절에서 먼저 적용된다.
     *
     * 따라서:
     *
     * 전체 61개
     * ↓
     * EPL 51개 필터링
     * ↓
     * 그 51개를 기준으로 50개씩 페이징
     *
     * 되는지를 검증한다.
     */
    @Test
    @DisplayName("리그 필터가 적용된 결과를 기준으로 페이징한다.")
    void searchTransfer_filterByLeague_paging() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                1000L,
                LeagueCode.EPL
        );

        Team chelsea = saveTeam(
                "Chelsea",
                2000L,
                LeagueCode.EPL
        );

        Team barcelona = saveTeam(
                "Barcelona",
                3000L,
                LeagueCode.LA_LIGA
        );

        Team realMadrid = saveTeam(
                "Real Madrid",
                4000L,
                LeagueCode.LA_LIGA
        );


        // EPL 이적 51개
        IntStream.rangeClosed(1, 51)
                .forEach(i -> {

                    Player player =
                            savePlayer(
                                    "EPL Player" + i,
                                    10_000L + i
                            );

                    saveTransfer(
                            player,
                            arsenal,
                            chelsea,
                            "Transfer",
                            LocalDate.of(2026, 2, 3)
                    );
                });


        // LA_LIGA 이적 10개
        // EPL 페이징 결과에 영향을 주면 안 된다.
        IntStream.rangeClosed(1, 10)
                .forEach(i -> {

                    Player player =
                            savePlayer(
                                    "LaLiga Player" + i,
                                    20_000L + i
                            );

                    saveTransfer(
                            player,
                            barcelona,
                            realMadrid,
                            "Transfer",
                            LocalDate.of(2026, 2, 3)
                    );
                });


        // when
        AllTransfersResponseDto firstPage =
                transferService.getTransfers(
                        0,
                        "",
                        LeagueCode.EPL
                ); // 1~50

        AllTransfersResponseDto secondPage =
                transferService.getTransfers(
                        1,
                        "",
                        LeagueCode.EPL
                ); // 51


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


        assertThat(firstPage.getTransfers())
                .extracting(TransferResponseDto::getPlayerName)
                .allMatch(name ->
                        name.startsWith("EPL Player")
                );

        assertThat(secondPage.getTransfers())
                .extracting(TransferResponseDto::getPlayerName)
                .allMatch(name ->
                        name.startsWith("EPL Player")
                );
    }


    // ==================================================
    // 예외 상황
    // ==================================================

    // [기존 수정]
    // leagueCode 인자만 추가.
    // page < 0 정책 자체는 변경 없음.
    @Test
    @DisplayName("페이지 번호가 음수이면 이적 정보를 검색할 수 없다.")
    void searchTransfer_negativePage_fail() {

        // given
        int page = -1;

        // when & then
        assertThatThrownBy(() ->
                transferService.getTransfers(
                        page,
                        "Saka",
                        null
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

    // [기존 수정]
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
                        "   Saka   ",
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


    // [기존 수정]
    // 검색 결과가 없더라도 예외가 아니라 빈 리스트 반환.
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
                        "Messi",
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


    // [기존 수정]
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
                        "   ",
                        null
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


    // [기존 수정]
    // keyWord == null이면 Service에서 ""로 정규화한다.
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
                        null,
                        null
                );

        // then
        assertThat(response.getTransfers())
                .hasSize(2);
    }


    // [기존 수정]
    @Test
    @DisplayName("이적 정보가 정확히 50개이면 다음 페이지가 존재하지 않는다.")
    void searchTransfer_exactPageSize() {

        // given
        IntStream.rangeClosed(1, 50)
                .forEach(i -> {

                    Player player =
                            savePlayer(
                                    "player" + i,
                                    Long.valueOf(i)
                            );

                    Team inTeam =
                            saveTeam(
                                    "inteam" + i,
                                    Long.valueOf(i + 100)
                            );

                    Team outTeam =
                            saveTeam(
                                    "outteam" + i,
                                    Long.valueOf(i + 200)
                            );

                    saveTransfer(
                            player,
                            outTeam,
                            inTeam,
                            "transfer",
                            LocalDate.of(2026, 2, 3)
                    );
                });


        // when
        AllTransfersResponseDto response =
                transferService.getTransfers(
                        0,
                        "",
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


    // [기존 수정]
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
        );

        // when
        AllTransfersResponseDto response =
                transferService.getTransfers(
                        100, // 5000~
                        "",
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
    // Fixture
    // ==================================================

    // [기존 유지]
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


    // [기존 유지]
    // 기존 테스트는 leagueCode가 필요 없기 때문에 그대로 둔다.
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
     * [신규 Fixture]
     *
     * 리그 필터 테스트에서는 Team.leagueCode가 실제로 존재해야 한다.
     *
     * 기존 saveTeam()을 변경하면 기존 테스트까지
     * 불필요하게 LeagueCode를 넣어야 하기 때문에
     * overload로 추가했다.
     */
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


    // [기존 유지]
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