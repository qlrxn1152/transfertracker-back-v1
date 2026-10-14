package com.dhoon.transfertracker.internal.transfer.query.service.impl;

import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.exception.InvalidTeamIdException;
import com.dhoon.transfertracker.internal.team.exception.NotFoundTeamException;
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
class TeamTransfersQueryTest {


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
    // 기본 조회
    // ==================================================

    @Test
    @DisplayName("해당 팀으로 들어온 선수의 이적 정보를 조회할 수 있다.")
    void getTeamTransfers_inTeam() {

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
                transferQueryOrchestrationService.getTeamTransfers(
                        arsenal.getId(),
                        0,
                        null
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
                transferQueryOrchestrationService.getTeamTransfers(
                        arsenal.getId(),
                        0,
                        null
                );


        // then
        assertThat(response.getTransfers())
                .hasSize(1);


        TransferResponseDto transfer =
                response.getTransfers().get(0);


        assertThat(transfer.getPlayerName())
                .isEqualTo("Bukayo Saka");

        assertThat(transfer.getInTeamName())
                .isEqualTo("Chelsea");

        assertThat(transfer.getOutTeamName())
                .isEqualTo("Arsenal");
    }


    @Test
    @DisplayName("팀의 영입과 방출 이적을 모두 조회한다.")
    void getTeamTransfers_inAndOut() {

        // given
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


        Player saka = savePlayer(
                "Bukayo Saka",
                10L
        );

        Player odegaard = savePlayer(
                "Martin Odegaard",
                20L
        );


        // Arsenal 영입
        saveTransfer(
                saka,
                arsenal,
                chelsea,
                "Transfer",
                LocalDate.of(2026, 7, 1)
        );

        // Arsenal 방출
        saveTransfer(
                odegaard,
                barcelona,
                arsenal,
                "Transfer",
                LocalDate.of(2025, 7, 1)
        );


        // when
        AllTransfersResponseDto response =
                transferQueryOrchestrationService.getTeamTransfers(
                        arsenal.getId(),
                        0,
                        null
                );


        // then
        assertThat(response.getTransfers())
                .hasSize(2);

        assertThat(response.getTransfers())
                .extracting(TransferResponseDto::getPlayerName)
                .containsExactly(
                        "Bukayo Saka",
                        "Martin Odegaard"
                );
    }


    @Test
    @DisplayName("해당 팀과 관계없는 이적 정보는 조회하지 않는다.")
    void getTeamTransfers_excludeOtherTeams() {

        // given
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


        Player saka = savePlayer(
                "Bukayo Saka",
                10L
        );

        Player pedri = savePlayer(
                "Pedri",
                20L
        );


        saveTransfer(
                saka,
                arsenal,
                chelsea,
                "Arsenal Transfer",
                LocalDate.of(2026, 7, 1)
        );


        saveTransfer(
                pedri,
                barcelona,
                chelsea,
                "Other Transfer",
                LocalDate.of(2026, 8, 1)
        );


        // when
        AllTransfersResponseDto response =
                transferQueryOrchestrationService.getTeamTransfers(
                        arsenal.getId(),
                        0,
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


    // ==================================================
    // DTO Mapping
    // ==================================================

    @Test
    @DisplayName("팀 이적 조회 결과는 연관 Entity를 정상적으로 DTO로 변환한다.")
    void getTeamTransfers_dtoMapping() {

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
                        LocalDate.of(2026, 8, 1)
                );


        // when
        AllTransfersResponseDto response =
                transferQueryOrchestrationService.getTeamTransfers(
                        arsenal.getId(),
                        0,
                        null
                );


        // then
        TransferResponseDto transfer =
                response.getTransfers().get(0);


        assertThat(transfer.getTransferId())
                .isEqualTo(savedTransfer.getId());

        assertThat(transfer.getPlayerId())
                .isEqualTo(saka.getId());

        assertThat(transfer.getPlayerName())
                .isEqualTo("Bukayo Saka");

        assertThat(transfer.getPhotoUrl())
                .isEqualTo(
                        "https://media.api-sports.io/football/players/10.png"
                );


        assertThat(transfer.getInTeamId())
                .isEqualTo(arsenal.getId());

        assertThat(transfer.getInTeamName())
                .isEqualTo("Arsenal");


        assertThat(transfer.getOutTeamId())
                .isEqualTo(chelsea.getId());

        assertThat(transfer.getOutTeamName())
                .isEqualTo("Chelsea");


        assertThat(transfer.getDate())
                .isEqualTo(
                        LocalDate.of(2026, 8, 1)
                );

        assertThat(transfer.getType())
                .isEqualTo("Permanent");
    }


    @Test
    @DisplayName("한글 이름이 존재하면 선수와 팀의 displayName을 반환한다.")
    void getTeamTransfers_withKoreanNames() {

        // given
        Team arsenal = Team.of(
                "Arsenal",
                42L
        );

        arsenal.assignKoTeamName(
                "아스널"
        );

        arsenal = teamRepository.save(
                arsenal
        );


        Team chelsea = Team.of(
                "Chelsea",
                49L
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
                transferQueryOrchestrationService.getTeamTransfers(
                        arsenal.getId(),
                        0,
                        null
                );


        // then
        TransferResponseDto transfer =
                response.getTransfers().get(0);


        assertThat(transfer.getPlayerName())
                .isEqualTo("부카요 사카");

        assertThat(transfer.getInTeamName())
                .isEqualTo("아스널");

        assertThat(transfer.getOutTeamName())
                .isEqualTo("첼시");
    }


    // ==================================================
    // 검색
    // ==================================================

    @Test
    @DisplayName("선수 이름 일부를 이용해 팀 이적 정보를 검색할 수 있다.")
    void getTeamTransfers_searchByPlayerName() {

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
                transferQueryOrchestrationService.getTeamTransfers(
                        arsenal.getId(),
                        0,
                        "Saka"
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
    @DisplayName("팀 이적 검색은 선수 이름의 대소문자를 구분하지 않는다.")
    void getTeamTransfers_ignoreCase() {

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
                transferQueryOrchestrationService.getTeamTransfers(
                        arsenal.getId(),
                        0,
                        "sAkA"
                );


        // then
        assertThat(response.getTransfers())
                .hasSize(1);

        assertThat(response.getTransfers().get(0).getPlayerName())
                .isEqualTo("Bukayo Saka");
    }


    @Test
    @DisplayName("한글 선수 이름으로도 팀 이적 정보를 검색할 수 있다.")
    void getTeamTransfers_searchByKoreanPlayerName() {

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
                transferQueryOrchestrationService.getTeamTransfers(
                        arsenal.getId(),
                        0,
                        "사카"
                );


        // then
        assertThat(response.getTransfers())
                .hasSize(1);

        assertThat(response.getTransfers().get(0).getPlayerName())
                .isEqualTo("부카요 사카");
    }


    @Test
    @DisplayName("검색어 앞뒤 공백을 제거하고 팀 이적 정보를 검색한다.")
    void getTeamTransfers_trimKeyword() {

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
                transferQueryOrchestrationService.getTeamTransfers(
                        arsenal.getId(),
                        0,
                        "   Saka   "
                );


        // then
        assertThat(response.getTransfers())
                .hasSize(1);
    }


    @Test
    @DisplayName("검색어가 null이면 해당 팀의 전체 이적 정보를 조회한다.")
    void getTeamTransfers_nullKeyword_findAll() {

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
                transferQueryOrchestrationService.getTeamTransfers(
                        arsenal.getId(),
                        0,
                        null
                );


        // then
        assertThat(response.getTransfers())
                .hasSize(2);
    }


    // ==================================================
    // 정렬
    // ==================================================

    @Test
    @DisplayName("팀 이적 정보는 이적 날짜 내림차순으로 조회된다.")
    void getTeamTransfers_sortByTransferDateDesc() {

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

        Player rice = savePlayer(
                "Declan Rice",
                30L
        );


        saveTransfer(
                saka,
                arsenal,
                chelsea,
                "2022",
                LocalDate.of(2022, 7, 1)
        );

        saveTransfer(
                odegaard,
                arsenal,
                chelsea,
                "2026",
                LocalDate.of(2026, 7, 1)
        );

        saveTransfer(
                rice,
                arsenal,
                chelsea,
                "2024",
                LocalDate.of(2024, 7, 1)
        );


        // when
        AllTransfersResponseDto response =
                transferQueryOrchestrationService.getTeamTransfers(
                        arsenal.getId(),
                        0,
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
    void getTeamTransfers_sameDate_sortByIdDesc() {

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
                transferQueryOrchestrationService.getTeamTransfers(
                        arsenal.getId(),
                        0,
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
    @DisplayName("팀 이적 정보가 50개를 초과하면 다음 페이지가 존재한다.")
    void getTeamTransfers_paging() {

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
                transferQueryOrchestrationService.getTeamTransfers(
                        arsenal.getId(),
                        0,
                        null
                );


        AllTransfersResponseDto secondPage =
                transferQueryOrchestrationService.getTeamTransfers(
                        arsenal.getId(),
                        1,
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
    @DisplayName("팀 이적 정보가 정확히 50개이면 다음 페이지가 존재하지 않는다.")
    void getTeamTransfers_exactPageSize() {

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
                transferQueryOrchestrationService.getTeamTransfers(
                        arsenal.getId(),
                        0,
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
    void getTeamTransfers_pageOutOfRange() {

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
                transferQueryOrchestrationService.getTeamTransfers(
                        arsenal.getId(),
                        100,
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
    // 빈 결과
    // ==================================================

    @Test
    @DisplayName("팀은 존재하지만 이적 기록이 없으면 빈 리스트를 반환한다.")
    void getTeamTransfers_empty() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                42L
        );


        // when
        AllTransfersResponseDto response =
                transferQueryOrchestrationService.getTeamTransfers(
                        arsenal.getId(),
                        0,
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
    @DisplayName("검색 조건에 맞는 선수가 없으면 빈 리스트를 반환한다.")
    void getTeamTransfers_notFoundKeyword() {

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
                transferQueryOrchestrationService.getTeamTransfers(
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


    // ==================================================
    // 실패
    // ==================================================

    @Test
    @DisplayName("페이지 번호가 음수이면 예외가 발생한다.")
    void getTeamTransfers_negativePage() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                42L
        );


        // when & then
        assertThatThrownBy(() ->
                transferQueryOrchestrationService.getTeamTransfers(
                        arsenal.getId(),
                        -1,
                        null
                )
        )
                .isInstanceOf(
                        InvalidTransferSearchPageValueException.class
                );
    }


    @Test
    @DisplayName("teamId가 null이면 예외가 발생한다.")
    void getTeamTransfers_nullTeamId() {

        // when & then
        assertThatThrownBy(() ->
                transferQueryOrchestrationService.getTeamTransfers(
                        null,
                        0,
                        null
                )
        )
                .isInstanceOf(
                        InvalidTeamIdException.class
                );
    }


    @Test
    @DisplayName("존재하지 않는 팀의 이적 정보를 조회하면 예외가 발생한다.")
    void getTeamTransfers_teamNotFound() {

        // given
        Long teamId = 999999L;


        // when & then
        assertThatThrownBy(() ->
                transferQueryOrchestrationService.getTeamTransfers(
                        teamId,
                        0,
                        null
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