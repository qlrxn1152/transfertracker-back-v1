package com.dhoon.transfertracker.internal.transfer.query.service.impl;

import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.transfer.domain.Transfer;
import com.dhoon.transfertracker.internal.transfer.exception.InvalidTransferSearchPageValueException;
import com.dhoon.transfertracker.internal.transfer.query.dto.response.AllTransfersResponseDto;
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
class TransferSearchQueryTest {


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
    // 기본 검색
    // ==================================================

    @Test
    @DisplayName("선수 이름의 일부를 이용해서 이적 정보를 검색할 수 있다.")
    void getTransfers_byPartialPlayerName() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                42L
        );

        Team chelsea = saveTeam(
                "Chelsea",
                49L
        );


        Player saka = savePlayer(
                "Bukayo Saka",
                10L
        );

        Player odegaard = savePlayer(
                "Martin Odegaard",
                20L
        );


        saveTransfer(
                saka,
                arsenal,
                chelsea,
                "Saka Transfer",
                LocalDate.of(2026, 7, 1)
        );

        saveTransfer(
                odegaard,
                arsenal,
                chelsea,
                "Odegaard Transfer",
                LocalDate.of(2025, 7, 1)
        );


        // when
        AllTransfersResponseDto response =
                transferQueryOrchestrationService.getTransfers(
                        0,
                        "Saka",
                        null,
                        null
                );


        // then
        assertThat(response.getTransfers())
                .hasSize(1);

        assertThat(response.getTransfers())
                .extracting(TransferResponseDto::getPlayerName)
                .containsExactly(
                        "Bukayo Saka"
                );
    }


    @Test
    @DisplayName("선수 이름 검색은 대소문자를 구분하지 않는다.")
    void getTransfers_ignoreCase() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                42L
        );

        Team chelsea = saveTeam(
                "Chelsea",
                49L
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
                transferQueryOrchestrationService.getTransfers(
                        0,
                        "sAkA",
                        null,
                        null
                );


        // then
        assertThat(response.getTransfers())
                .hasSize(1);

        assertThat(response.getTransfers().get(0).getPlayerName())
                .isEqualTo(
                        "Bukayo Saka"
                );
    }


    @Test
    @DisplayName("한글 선수 이름으로도 이적 정보를 검색할 수 있다.")
    void getTransfers_byKoreanPlayerName() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                42L
        );

        Team chelsea = saveTeam(
                "Chelsea",
                49L
        );


        Player saka = Player.of(
                "Bukayo Saka",
                10L
        );

        saka.assignKoPlayerName(
                "부카요 사카"
        );

        saka = playerRepository.save(
                saka
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
                transferQueryOrchestrationService.getTransfers(
                        0,
                        "사카",
                        null,
                        null
                );


        // then
        assertThat(response.getTransfers())
                .hasSize(1);

        assertThat(response.getTransfers().get(0).getPlayerName())
                .isEqualTo(
                        "부카요 사카"
                );
    }


    @Test
    @DisplayName("검색어 앞뒤 공백을 제거하고 검색한다.")
    void getTransfers_trimKeyword() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                42L
        );

        Team chelsea = saveTeam(
                "Chelsea",
                49L
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
                transferQueryOrchestrationService.getTransfers(
                        0,
                        "   Saka   ",
                        null,
                        null
                );


        // then
        assertThat(response.getTransfers())
                .hasSize(1);
    }


    @Test
    @DisplayName("검색어와 필터가 모두 null이면 전체 이적 정보를 조회한다.")
    void getTransfers_nullFilters_findAll() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                42L
        );

        Team chelsea = saveTeam(
                "Chelsea",
                49L
        );


        Player saka = savePlayer(
                "Bukayo Saka",
                10L
        );

        Player odegaard = savePlayer(
                "Martin Odegaard",
                20L
        );


        saveTransfer(
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
        AllTransfersResponseDto response =
                transferQueryOrchestrationService.getTransfers(
                        0,
                        null,
                        null,
                        null
                );


        // then
        assertThat(response.getTransfers())
                .hasSize(2);
    }


    // ==================================================
    // DTO Mapping
    // ==================================================

    @Test
    @DisplayName("이적 검색 결과는 선수와 입출 팀 정보를 정상적으로 DTO로 변환한다.")
    void getTransfers_dtoMapping() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                42L
        );

        Team chelsea = saveTeam(
                "Chelsea",
                49L
        );

        Player saka = savePlayer(
                "Bukayo Saka",
                10L
        );


        Transfer savedTransfer =
                saveTransfer(
                        saka,
                        arsenal,
                        chelsea,
                        "Permanent",
                        LocalDate.of(2026, 7, 1)
                );


        // when
        AllTransfersResponseDto response =
                transferQueryOrchestrationService.getTransfers(
                        0,
                        null,
                        null,
                        null
                );


        // then
        TransferResponseDto transfer =
                response.getTransfers().get(0);


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


        assertThat(transfer.getDate())
                .isEqualTo(
                        LocalDate.of(2026, 7, 1)
                );

        assertThat(transfer.getType())
                .isEqualTo(
                        "Permanent"
                );
    }


    @Test
    @DisplayName("한글 이름이 존재하면 검색 결과에서 displayName을 반환한다.")
    void getTransfers_withKoreanNames() {

        // given
        Team arsenal = Team.of(
                "Arsenal",
                42L,
                LeagueCode.EPL
        );

        arsenal.assignKoTeamName(
                "아스널"
        );

        arsenal = teamRepository.save(
                arsenal
        );


        Team chelsea = Team.of(
                "Chelsea",
                49L,
                LeagueCode.EPL
        );

        chelsea.assignKoTeamName(
                "첼시"
        );

        chelsea = teamRepository.save(
                chelsea
        );


        Player saka = Player.of(
                "Bukayo Saka",
                10L
        );

        saka.assignKoPlayerName(
                "부카요 사카"
        );

        saka = playerRepository.save(
                saka
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
                transferQueryOrchestrationService.getTransfers(
                        0,
                        null,
                        null,
                        null
                );


        // then
        TransferResponseDto transfer =
                response.getTransfers().get(0);


        assertThat(transfer.getPlayerName())
                .isEqualTo(
                        "부카요 사카"
                );

        assertThat(transfer.getInTeamName())
                .isEqualTo(
                        "아스널"
                );

        assertThat(transfer.getOutTeamName())
                .isEqualTo(
                        "첼시"
                );
    }


    // ==================================================
    // League Filter
    // ==================================================

    @Test
    @DisplayName("inTeam이 선택한 리그에 속하면 이적 정보를 조회한다.")
    void getTransfers_filterByLeague_inTeam() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                42L,
                LeagueCode.EPL
        );

        Team barcelona = saveTeam(
                "Barcelona",
                529L,
                LeagueCode.LA_LIGA
        );

        Player saka = savePlayer(
                "Bukayo Saka",
                10L
        );


        saveTransfer(
                saka,
                arsenal,
                barcelona,
                "Transfer",
                LocalDate.of(2026, 7, 1)
        );


        // when
        AllTransfersResponseDto response =
                transferQueryOrchestrationService.getTransfers(
                        0,
                        null,
                        LeagueCode.EPL,
                        null
                );


        // then
        assertThat(response.getTransfers())
                .hasSize(1);
    }


    @Test
    @DisplayName("outTeam이 선택한 리그에 속하면 이적 정보를 조회한다.")
    void getTransfers_filterByLeague_outTeam() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                42L,
                LeagueCode.EPL
        );

        Team barcelona = saveTeam(
                "Barcelona",
                529L,
                LeagueCode.LA_LIGA
        );

        Player saka = savePlayer(
                "Bukayo Saka",
                10L
        );


        saveTransfer(
                saka,
                barcelona,
                arsenal,
                "Transfer",
                LocalDate.of(2026, 7, 1)
        );


        // when
        AllTransfersResponseDto response =
                transferQueryOrchestrationService.getTransfers(
                        0,
                        null,
                        LeagueCode.EPL,
                        null
                );


        // then
        assertThat(response.getTransfers())
                .hasSize(1);
    }


    @Test
    @DisplayName("선택한 리그와 관계없는 이적 정보는 조회하지 않는다.")
    void getTransfers_filterByLeague_excludeOtherLeague() {

        // given
        Team barcelona = saveTeam(
                "Barcelona",
                529L,
                LeagueCode.LA_LIGA
        );

        Team realMadrid = saveTeam(
                "Real Madrid",
                541L,
                LeagueCode.LA_LIGA
        );

        Player pedri = savePlayer(
                "Pedri",
                10L
        );


        saveTransfer(
                pedri,
                barcelona,
                realMadrid,
                "Transfer",
                LocalDate.of(2026, 7, 1)
        );


        // when
        AllTransfersResponseDto response =
                transferQueryOrchestrationService.getTransfers(
                        0,
                        null,
                        LeagueCode.EPL,
                        null
                );


        // then
        assertThat(response.getTransfers())
                .isEmpty();
    }


    // ==================================================
    // Team Filter
    // ==================================================

    @Test
    @DisplayName("inTeam이 선택한 팀이면 이적 정보를 조회한다.")
    void getTransfers_filterByInTeam() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                42L
        );

        Team chelsea = saveTeam(
                "Chelsea",
                49L
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
                transferQueryOrchestrationService.getTransfers(
                        0,
                        null,
                        null,
                        arsenal.getId()
                );


        // then
        assertThat(response.getTransfers())
                .hasSize(1);
    }


    @Test
    @DisplayName("outTeam이 선택한 팀이면 이적 정보를 조회한다.")
    void getTransfers_filterByOutTeam() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                42L
        );

        Team chelsea = saveTeam(
                "Chelsea",
                49L
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
                transferQueryOrchestrationService.getTransfers(
                        0,
                        null,
                        null,
                        arsenal.getId()
                );


        // then
        assertThat(response.getTransfers())
                .hasSize(1);
    }


    @Test
    @DisplayName("존재하지 않는 팀 ID로 필터링하면 빈 리스트를 반환한다.")
    void getTransfers_nonexistentTeam_findEmpty() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                42L
        );

        Team chelsea = saveTeam(
                "Chelsea",
                49L
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
                transferQueryOrchestrationService.getTransfers(
                        0,
                        null,
                        null,
                        999999L
                );


        // then
        assertThat(response.getTransfers())
                .isEmpty();
    }


    // ==================================================
    // 복합 조건
    // ==================================================

    @Test
    @DisplayName("리그, 팀, 선수 이름을 모두 만족하는 이적 정보만 조회한다.")
    void getTransfers_filterByLeagueAndTeamAndKeyword() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                42L,
                LeagueCode.EPL
        );

        Team chelsea = saveTeam(
                "Chelsea",
                49L,
                LeagueCode.EPL
        );

        Team barcelona = saveTeam(
                "Barcelona",
                529L,
                LeagueCode.LA_LIGA
        );


        Player saka = savePlayer(
                "Bukayo Saka",
                10L
        );

        Player odegaard = savePlayer(
                "Martin Odegaard",
                20L
        );

        Player pedri = savePlayer(
                "Pedri",
                30L
        );


        // 모든 조건 만족
        saveTransfer(
                saka,
                arsenal,
                chelsea,
                "Target Transfer",
                LocalDate.of(2026, 7, 1)
        );


        // 선수 이름 불일치
        saveTransfer(
                odegaard,
                arsenal,
                chelsea,
                "Wrong Player",
                LocalDate.of(2025, 7, 1)
        );


        // 리그 / 팀 불일치
        saveTransfer(
                pedri,
                barcelona,
                chelsea,
                "Wrong League",
                LocalDate.of(2024, 7, 1)
        );


        // when
        AllTransfersResponseDto response =
                transferQueryOrchestrationService.getTransfers(
                        0,
                        "Saka",
                        LeagueCode.EPL,
                        arsenal.getId()
                );


        // then
        assertThat(response.getTransfers())
                .hasSize(1);

        assertThat(response.getTransfers().get(0).getType())
                .isEqualTo(
                        "Target Transfer"
                );
    }


    // ==================================================
    // 정렬
    // ==================================================

    @Test
    @DisplayName("이적 정보는 이적 날짜 내림차순으로 조회된다.")
    void getTransfers_sortByTransferDateDesc() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                42L
        );

        Team chelsea = saveTeam(
                "Chelsea",
                49L
        );


        Player player1 = savePlayer(
                "Player 1",
                10L
        );

        Player player2 = savePlayer(
                "Player 2",
                20L
        );

        Player player3 = savePlayer(
                "Player 3",
                30L
        );


        saveTransfer(
                player1,
                arsenal,
                chelsea,
                "2022",
                LocalDate.of(2022, 7, 1)
        );

        saveTransfer(
                player2,
                arsenal,
                chelsea,
                "2026",
                LocalDate.of(2026, 7, 1)
        );

        saveTransfer(
                player3,
                arsenal,
                chelsea,
                "2024",
                LocalDate.of(2024, 7, 1)
        );


        // when
        AllTransfersResponseDto response =
                transferQueryOrchestrationService.getTransfers(
                        0,
                        null,
                        null,
                        null
                );


        // then
        assertThat(response.getTransfers())
                .extracting(TransferResponseDto::getDate)
                .containsExactly(
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2024, 7, 1),
                        LocalDate.of(2022, 7, 1)
                );
    }


    @Test
    @DisplayName("이적 날짜가 같으면 ID 내림차순으로 조회된다.")
    void getTransfers_sameDate_sortByIdDesc() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                42L
        );

        Team chelsea = saveTeam(
                "Chelsea",
                49L
        );


        Player saka = savePlayer(
                "Bukayo Saka",
                10L
        );

        Player odegaard = savePlayer(
                "Martin Odegaard",
                20L
        );


        LocalDate sameDate =
                LocalDate.of(2026, 7, 1);


        saveTransfer(
                saka,
                arsenal,
                chelsea,
                "First",
                sameDate
        );

        saveTransfer(
                odegaard,
                arsenal,
                chelsea,
                "Second",
                sameDate
        );


        // when
        AllTransfersResponseDto response =
                transferQueryOrchestrationService.getTransfers(
                        0,
                        null,
                        null,
                        null
                );


        // then
        assertThat(response.getTransfers())
                .extracting(TransferResponseDto::getType)
                .containsExactly(
                        "Second",
                        "First"
                );
    }


    // ==================================================
    // Paging
    // ==================================================

    @Test
    @DisplayName("검색 결과가 50개를 초과하면 다음 페이지가 존재한다.")
    void getTransfers_paging() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                42L
        );

        Team chelsea = saveTeam(
                "Chelsea",
                49L
        );


        for (int i = 1; i <= 51; i++) {

            Player player = savePlayer(
                    "Player " + i,
                    1000L + i
            );

            saveTransfer(
                    player,
                    arsenal,
                    chelsea,
                    "Transfer " + i,
                    LocalDate.of(2026, 1, 1)
                            .plusDays(i)
            );
        }


        // when
        AllTransfersResponseDto firstPage =
                transferQueryOrchestrationService.getTransfers(
                        0,
                        null,
                        null,
                        null
                );

        AllTransfersResponseDto secondPage =
                transferQueryOrchestrationService.getTransfers(
                        1,
                        null,
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
    void getTransfers_exactPageSize() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                42L
        );

        Team chelsea = saveTeam(
                "Chelsea",
                49L
        );


        for (int i = 1; i <= 50; i++) {

            Player player = savePlayer(
                    "Player " + i,
                    2000L + i
            );

            saveTransfer(
                    player,
                    arsenal,
                    chelsea,
                    "Transfer " + i,
                    LocalDate.of(2026, 1, 1)
                            .plusDays(i)
            );
        }


        // when
        AllTransfersResponseDto response =
                transferQueryOrchestrationService.getTransfers(
                        0,
                        null,
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
    void getTransfers_pageOutOfRange() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                42L
        );

        Team chelsea = saveTeam(
                "Chelsea",
                49L
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
                transferQueryOrchestrationService.getTransfers(
                        10,
                        null,
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
    // 빈 결과 / 실패
    // ==================================================

    @Test
    @DisplayName("검색 조건에 맞는 이적 정보가 없으면 빈 리스트를 반환한다.")
    void getTransfers_notFound() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                42L
        );

        Team chelsea = saveTeam(
                "Chelsea",
                49L
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
                transferQueryOrchestrationService.getTransfers(
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


    @Test
    @DisplayName("페이지 번호가 음수이면 예외가 발생한다.")
    void getTransfers_negativePage() {

        // when & then
        assertThatThrownBy(() ->
                transferQueryOrchestrationService.getTransfers(
                        -1,
                        null,
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