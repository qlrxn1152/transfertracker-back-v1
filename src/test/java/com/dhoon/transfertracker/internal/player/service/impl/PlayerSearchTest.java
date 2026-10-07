package com.dhoon.transfertracker.internal.player.service.impl;

import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.dto.response.PlayerItemResponseDto;
import com.dhoon.transfertracker.internal.player.dto.response.PlayersResponseDto;
import com.dhoon.transfertracker.internal.player.exception.InvalidPlayerSearchPageValueException;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.teamplayer.domain.TeamPlayer;
import com.dhoon.transfertracker.internal.teamplayer.repository.TeamPlayerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@DataJpaTest
@Import({
        PlayerOrchestrationService.class,
        PlayerTxService.class
})
@ActiveProfiles("test")

/*
 * [기존 수정 - 중요]
 *
 * @DataJpaTest는 기본적으로 각각의 테스트를
 * 하나의 Transaction으로 감싼다.
 *
 * 하지만 실제 Player 조회 구조는:
 *
 * Client
 *      ↓
 * Controller
 *      ↓
 * PlayerOrchestrationService
 *      → Transaction X
 *      → Validation
 *      → Keyword 정규화
 *      → Pageable 생성
 *
 *      ↓
 *
 * PlayerTxService
 *      → Transaction O
 *      → Repository 조회
 *      → Entity 사용
 *      → DTO 생성
 *
 *      ↓
 *
 * Transaction 종료
 *
 *      ↓
 *
 * PlayerOrchestrationService
 *      → DTO 반환
 *
 *
 * 실제 Transaction 경계를 테스트하기 위해
 * Test 자체 Transaction은 사용하지 않는다.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PlayerSearchTest {

    /*
     * 실제 요청 흐름의 Service 진입점.
     *
     * 테스트에서 PlayerTxService를 직접 호출하지 않는다.
     */
    @Autowired
    PlayerOrchestrationService playerOrchestrationService;

    @Autowired
    PlayerRepository playerRepository;

    @Autowired
    TeamRepository teamRepository;

    @Autowired
    TeamPlayerRepository teamPlayerRepository;


    /*
     * Test Transaction을 꺼두었기 때문에
     * 테스트 종료 후 자동 Rollback되지 않는다.
     *
     * 따라서 FK 자식 Entity부터 직접 삭제한다.
     */
    @AfterEach
    void clearDatabase() {

        teamPlayerRepository.deleteAll();
        playerRepository.deleteAll();
        teamRepository.deleteAll();
    }


    // ==================================================
    // 정상 상황
    // ==================================================

    /*
     * [기존 수정]
     *
     * 현재 선수 목록은 Player가 아니라
     * TeamPlayer를 기준으로 조회한다.
     *
     * 즉 현재 팀에 소속된 선수만
     * 선수 목록에 노출된다.
     */
    @Test
    @DisplayName("선수 이름의 일부를 이용해서 검색할 수 있다.")
    void searchPlayer_byPartialName() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                100L,
                LeagueCode.EPL
        );

        savePlayerWithTeam(
                "Son Heung-min",
                1L,
                arsenal
        );

        savePlayerWithTeam(
                "Bukayo Saka",
                2L,
                arsenal
        );

        savePlayerWithTeam(
                "Bruno Fernandes",
                3L,
                arsenal
        );


        // when
        PlayersResponseDto response =
                playerOrchestrationService.getPlayers(
                        0,
                        "Heung",
                        null,
                        null
                );


        // then
        assertThat(response.getPlayers())
                .hasSize(1);

        assertThat(response.getPlayers())
                .extracting(
                        PlayerItemResponseDto::getPlayerName
                )
                .containsExactly(
                        "Son Heung-min"
                );
    }


    /*
     * [기존 유지]
     */
    @Test
    @DisplayName("선수 이름 검색은 대소문자를 구분하지 않는다.")
    void searchPlayer_ignoreCase() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                100L,
                LeagueCode.EPL
        );

        savePlayerWithTeam(
                "Bukayo Saka",
                1L,
                arsenal
        );

        savePlayerWithTeam(
                "Son Heung-min",
                2L,
                arsenal
        );


        // when
        PlayersResponseDto response =
                playerOrchestrationService.getPlayers(
                        0,
                        "sAkA",
                        null,
                        null
                );


        // then
        assertThat(response.getPlayers())
                .extracting(
                        PlayerItemResponseDto::getPlayerName
                )
                .containsExactly(
                        "Bukayo Saka"
                );
    }


    /*
     * [기존 유지]
     *
     * PlayerOrchestrationService가 생성하는 Pageable의
     * 정렬 조건:
     *
     * player.playerName ASC
     * player.id ASC
     */
    @Test
    @DisplayName("선수 목록은 이름 오름차순으로 조회된다.")
    void searchPlayer_sortByName() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                100L,
                LeagueCode.EPL
        );

        savePlayerWithTeam(
                "Son Heung-min",
                1L,
                arsenal
        );

        savePlayerWithTeam(
                "Bruno Fernandes",
                2L,
                arsenal
        );

        savePlayerWithTeam(
                "Bukayo Saka",
                3L,
                arsenal
        );


        // when
        PlayersResponseDto response =
                playerOrchestrationService.getPlayers(
                        0,
                        "",
                        null,
                        null
                );


        // then
        assertThat(response.getPlayers())
                .extracting(
                        PlayerItemResponseDto::getPlayerName
                )
                .containsExactly(
                        "Bruno Fernandes",
                        "Bukayo Saka",
                        "Son Heung-min"
                );
    }


    /*
     * [기존 유지]
     *
     * 이름이 같다면 두 번째 정렬 조건인
     * player.id ASC를 사용한다.
     */
    @Test
    @DisplayName("선수 이름이 같으면 먼저 저장된 선수가 먼저 조회된다.")
    void searchPlayer_sameName_sortByIdAsc() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                100L,
                LeagueCode.EPL
        );

        Player firstPlayer =
                savePlayerWithTeam(
                        "Same Player",
                        1L,
                        arsenal
                );

        Player secondPlayer =
                savePlayerWithTeam(
                        "Same Player",
                        2L,
                        arsenal
                );


        // when
        PlayersResponseDto response =
                playerOrchestrationService.getPlayers(
                        0,
                        "Same Player",
                        null,
                        null
                );


        // then
        assertThat(response.getPlayers())
                .extracting(
                        PlayerItemResponseDto::getPlayerId
                )
                .containsExactly(
                        firstPlayer.getId(),
                        secondPlayer.getId()
                );
    }


    /*
     * [기존 유지 - 중요]
     *
     * PlayerOrchestrationService에서
     *
     * page size = 50
     *
     * 으로 Pageable을 만드는지
     * 실제 조회 결과로 검증한다.
     */
    @Test
    @DisplayName("검색 결과가 50개를 초과하면 Slice 페이징 정보가 정상적으로 반환된다.")
    void searchPlayer_paging() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                100L,
                LeagueCode.EPL
        );

        IntStream.rangeClosed(
                        1,
                        51
                )
                .forEach(i ->
                        savePlayerWithTeam(
                                String.format(
                                        "Player%02d",
                                        i
                                ),
                                (long) i,
                                arsenal
                        )
                );


        // when
        PlayersResponseDto firstPage =
                playerOrchestrationService.getPlayers(
                        0,
                        "player",
                        null,
                        null
                );

        PlayersResponseDto secondPage =
                playerOrchestrationService.getPlayers(
                        1,
                        "player",
                        null,
                        null
                );


        // then
        assertThat(firstPage.getPlayers())
                .hasSize(50);

        assertThat(firstPage.isHasNext())
                .isTrue();

        assertThat(firstPage.isHasPrevious())
                .isFalse();


        assertThat(secondPage.getPlayers())
                .hasSize(1);

        assertThat(secondPage.isHasNext())
                .isFalse();

        assertThat(secondPage.isHasPrevious())
                .isTrue();
    }


    /*
     * [기존 유지 - 경계]
     */
    @Test
    @DisplayName("검색 결과가 정확히 50개이면 다음 페이지가 존재하지 않는다.")
    void searchPlayer_exactPageSize() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                100L,
                LeagueCode.EPL
        );

        IntStream.rangeClosed(
                        1,
                        50
                )
                .forEach(i ->
                        savePlayerWithTeam(
                                String.format(
                                        "Player%02d",
                                        i
                                ),
                                (long) i,
                                arsenal
                        )
                );


        // when
        PlayersResponseDto response =
                playerOrchestrationService.getPlayers(
                        0,
                        "",
                        null,
                        null
                );


        // then
        assertThat(response.getPlayers())
                .hasSize(50);

        assertThat(response.isHasNext())
                .isFalse();

        assertThat(response.isHasPrevious())
                .isFalse();
    }


    /*
     * [기존 수정 - 중요]
     *
     * TxService 안에서:
     *
     * TeamPlayer
     * Player
     * Team
     *
     * 을 사용해서 DTO까지 만든 뒤
     * OrchestrationService로 반환하는지 확인한다.
     *
     * Test 자체 Transaction은 없기 때문에
     * Lazy Entity를 Tx 밖에서 사용하면 문제가 드러날 수 있다.
     */
    @Test
    @DisplayName("선수 검색 결과에 현재 소속팀 정보가 함께 포함된다.")
    void searchPlayer_mappingTeam() {

        // given
        Team arsenal =
                Team.of(
                        "Arsenal",
                        100L,
                        LeagueCode.EPL
                );

        arsenal.assignKoTeamName(
                "아스널"
        );

        arsenal =
                teamRepository.save(
                        arsenal
                );


        Player saka =
                savePlayerWithTeam(
                        "Bukayo Saka",
                        1L,
                        arsenal
                );


        // when
        PlayersResponseDto response =
                playerOrchestrationService.getPlayers(
                        0,
                        "Saka",
                        null,
                        null
                );


        // then
        PlayerItemResponseDto player =
                response
                        .getPlayers()
                        .get(0);


        assertThat(player.getPlayerId())
                .isEqualTo(
                        saka.getId()
                );

        assertThat(player.getPlayerName())
                .isEqualTo(
                        "Bukayo Saka"
                );

        assertThat(player.getTeamName())
                .isEqualTo(
                        "Arsenal"
                );

        assertThat(player.getTeamNameKo())
                .isEqualTo(
                        "아스널"
                );

        assertThat(player.getPhotoUrl())
                .isEqualTo(
                        "https://media.api-sports.io/football/players/1.png"
                );
    }


    // ==================================================
    // 리그 필터
    // ==================================================

    @Test
    @DisplayName("현재 소속팀이 선택한 리그에 속한 선수만 조회한다.")
    void searchPlayer_filterByLeague() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                100L,
                LeagueCode.EPL
        );

        Team barcelona = saveTeam(
                "Barcelona",
                200L,
                LeagueCode.LA_LIGA
        );

        savePlayerWithTeam(
                "Bukayo Saka",
                1L,
                arsenal
        );

        savePlayerWithTeam(
                "Pedri",
                2L,
                barcelona
        );


        // when
        PlayersResponseDto response =
                playerOrchestrationService.getPlayers(
                        0,
                        "",
                        LeagueCode.EPL,
                        null
                );


        // then
        assertThat(response.getPlayers())
                .hasSize(1);

        assertThat(response.getPlayers())
                .extracting(
                        PlayerItemResponseDto::getPlayerName
                )
                .containsExactly(
                        "Bukayo Saka"
                );
    }


    @Test
    @DisplayName("현재 소속팀이 선택한 리그가 아니면 선수를 조회하지 않는다.")
    void searchPlayer_filterByLeague_excludeOtherLeague() {

        // given
        Team barcelona = saveTeam(
                "Barcelona",
                100L,
                LeagueCode.LA_LIGA
        );

        savePlayerWithTeam(
                "Pedri",
                1L,
                barcelona
        );


        // when
        PlayersResponseDto response =
                playerOrchestrationService.getPlayers(
                        0,
                        "",
                        LeagueCode.EPL,
                        null
                );


        // then
        assertThat(response.getPlayers())
                .isEmpty();
    }


    @Test
    @DisplayName("리그 조건이 null이면 모든 리그의 소속 선수를 조회한다.")
    void searchPlayer_nullLeague_findAllLeague() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                100L,
                LeagueCode.EPL
        );

        Team barcelona = saveTeam(
                "Barcelona",
                200L,
                LeagueCode.LA_LIGA
        );

        savePlayerWithTeam(
                "Bukayo Saka",
                1L,
                arsenal
        );

        savePlayerWithTeam(
                "Pedri",
                2L,
                barcelona
        );


        // when
        PlayersResponseDto response =
                playerOrchestrationService.getPlayers(
                        0,
                        "",
                        null,
                        null
                );


        // then
        assertThat(response.getPlayers())
                .extracting(
                        PlayerItemResponseDto::getPlayerName
                )
                .containsExactly(
                        "Bukayo Saka",
                        "Pedri"
                );
    }


    @Test
    @DisplayName("리그와 선수 이름 조건을 모두 만족하는 선수만 조회한다.")
    void searchPlayer_filterByLeagueAndKeyword() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                100L,
                LeagueCode.EPL
        );

        Team manUnited = saveTeam(
                "Manchester United",
                200L,
                LeagueCode.EPL
        );

        Team barcelona = saveTeam(
                "Barcelona",
                300L,
                LeagueCode.LA_LIGA
        );


        // league O / keyword O
        savePlayerWithTeam(
                "Bukayo Saka",
                1L,
                arsenal
        );

        // league O / keyword X
        savePlayerWithTeam(
                "Bruno Fernandes",
                2L,
                manUnited
        );

        // league X / keyword O
        savePlayerWithTeam(
                "Saka Test",
                3L,
                barcelona
        );


        // when
        PlayersResponseDto response =
                playerOrchestrationService.getPlayers(
                        0,
                        "Saka",
                        LeagueCode.EPL,
                        null
                );


        // then
        assertThat(response.getPlayers())
                .hasSize(1);

        assertThat(response.getPlayers())
                .extracting(
                        PlayerItemResponseDto::getPlayerName
                )
                .containsExactly(
                        "Bukayo Saka"
                );
    }


    // ==================================================
    // 팀 필터
    // ==================================================

    @Test
    @DisplayName("선택한 팀에 현재 소속된 선수만 조회한다.")
    void searchPlayer_filterByTeam() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                100L,
                LeagueCode.EPL
        );

        Team chelsea = saveTeam(
                "Chelsea",
                200L,
                LeagueCode.EPL
        );

        savePlayerWithTeam(
                "Bukayo Saka",
                1L,
                arsenal
        );

        savePlayerWithTeam(
                "Cole Palmer",
                2L,
                chelsea
        );


        // when
        PlayersResponseDto response =
                playerOrchestrationService.getPlayers(
                        0,
                        "",
                        null,
                        arsenal.getId()
                );


        // then
        assertThat(response.getPlayers())
                .hasSize(1);

        PlayerItemResponseDto player =
                response
                        .getPlayers()
                        .get(0);

        assertThat(player.getPlayerName())
                .isEqualTo(
                        "Bukayo Saka"
                );

        assertThat(player.getTeamName())
                .isEqualTo(
                        "Arsenal"
                );
    }


    @Test
    @DisplayName("팀과 선수 이름 조건을 모두 만족하는 선수만 조회한다.")
    void searchPlayer_filterByTeamAndKeyword() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                100L,
                LeagueCode.EPL
        );

        Team chelsea = saveTeam(
                "Chelsea",
                200L,
                LeagueCode.EPL
        );


        // team O / keyword O
        savePlayerWithTeam(
                "Bukayo Saka",
                1L,
                arsenal
        );

        // team O / keyword X
        savePlayerWithTeam(
                "Martin Odegaard",
                2L,
                arsenal
        );

        // team X / keyword O
        savePlayerWithTeam(
                "Saka Test",
                3L,
                chelsea
        );


        // when
        PlayersResponseDto response =
                playerOrchestrationService.getPlayers(
                        0,
                        "Saka",
                        null,
                        arsenal.getId()
                );


        // then
        assertThat(response.getPlayers())
                .hasSize(1);

        assertThat(response.getPlayers())
                .extracting(
                        PlayerItemResponseDto::getPlayerName
                )
                .containsExactly(
                        "Bukayo Saka"
                );
    }


    // ==================================================
    // 복합 필터
    // ==================================================

    @Test
    @DisplayName("리그와 팀 조건을 모두 만족하는 선수만 조회한다.")
    void searchPlayer_filterByLeagueAndTeam() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                100L,
                LeagueCode.EPL
        );

        Team chelsea = saveTeam(
                "Chelsea",
                200L,
                LeagueCode.EPL
        );

        Team barcelona = saveTeam(
                "Barcelona",
                300L,
                LeagueCode.LA_LIGA
        );

        savePlayerWithTeam(
                "Bukayo Saka",
                1L,
                arsenal
        );

        savePlayerWithTeam(
                "Cole Palmer",
                2L,
                chelsea
        );

        savePlayerWithTeam(
                "Pedri",
                3L,
                barcelona
        );


        // when
        PlayersResponseDto response =
                playerOrchestrationService.getPlayers(
                        0,
                        "",
                        LeagueCode.EPL,
                        arsenal.getId()
                );


        // then
        assertThat(response.getPlayers())
                .hasSize(1);

        assertThat(response.getPlayers())
                .extracting(
                        PlayerItemResponseDto::getPlayerName
                )
                .containsExactly(
                        "Bukayo Saka"
                );
    }


    /*
     * [신규 - 중요]
     *
     * leagueCode와 teamId를 각각 독립적으로 OR 처리하는 것이 아니라
     * 최종 WHERE 조건에서는 AND 관계다.
     */
    @Test
    @DisplayName("선택한 리그와 팀이 서로 일치하지 않으면 빈 리스트를 반환한다.")
    void searchPlayer_filterByLeagueAndTeam_mismatch() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                100L,
                LeagueCode.EPL
        );

        savePlayerWithTeam(
                "Bukayo Saka",
                1L,
                arsenal
        );


        // when
        PlayersResponseDto response =
                playerOrchestrationService.getPlayers(
                        0,
                        "",
                        LeagueCode.LA_LIGA,
                        arsenal.getId()
                );


        // then
        assertThat(response.getPlayers())
                .isEmpty();

        assertThat(response.isHasNext())
                .isFalse();

        assertThat(response.isHasPrevious())
                .isFalse();
    }


    /*
     * [신규 - 중요]
     *
     * leagueCode
     * AND teamId
     * AND keyWord
     *
     * 세 조건을 모두 만족해야 한다.
     */
    @Test
    @DisplayName("리그, 팀, 선수 이름 조건을 모두 만족하는 선수만 조회한다.")
    void searchPlayer_filterByLeagueAndTeamAndKeyword() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                100L,
                LeagueCode.EPL
        );

        Team chelsea = saveTeam(
                "Chelsea",
                200L,
                LeagueCode.EPL
        );

        Team barcelona = saveTeam(
                "Barcelona",
                300L,
                LeagueCode.LA_LIGA
        );


        // 세 조건 모두 O
        savePlayerWithTeam(
                "Bukayo Saka",
                1L,
                arsenal
        );

        // league O / team O / keyword X
        savePlayerWithTeam(
                "Martin Odegaard",
                2L,
                arsenal
        );

        // league O / team X / keyword O
        savePlayerWithTeam(
                "Saka Chelsea",
                3L,
                chelsea
        );

        // league X / team X / keyword O
        savePlayerWithTeam(
                "Saka Barcelona",
                4L,
                barcelona
        );


        // when
        PlayersResponseDto response =
                playerOrchestrationService.getPlayers(
                        0,
                        "Saka",
                        LeagueCode.EPL,
                        arsenal.getId()
                );


        // then
        assertThat(response.getPlayers())
                .hasSize(1);

        PlayerItemResponseDto player =
                response
                        .getPlayers()
                        .get(0);

        assertThat(player.getPlayerName())
                .isEqualTo(
                        "Bukayo Saka"
                );

        assertThat(player.getTeamName())
                .isEqualTo(
                        "Arsenal"
                );
    }


    /*
     * 현재 정책:
     *
     * 존재하지 않는 teamId를 검색 조건으로 전달한다고 해서
     * NotFoundTeamException을 발생시키지 않는다.
     *
     * 검색 조건에 맞는 선수가 없으므로 빈 결과를 반환한다.
     */
    @Test
    @DisplayName("존재하지 않는 팀으로 필터링하면 빈 리스트를 반환한다.")
    void searchPlayer_unknownTeamId_empty() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                100L,
                LeagueCode.EPL
        );

        savePlayerWithTeam(
                "Bukayo Saka",
                1L,
                arsenal
        );


        // when
        PlayersResponseDto response =
                playerOrchestrationService.getPlayers(
                        0,
                        "",
                        null,
                        999999L
                );


        // then
        assertThat(response.getPlayers())
                .isEmpty();

        assertThat(response.isHasNext())
                .isFalse();
    }


    // ==================================================
    // 필터 + 페이징
    // ==================================================

    /*
     * [신규 - 중요]
     *
     * Page 조회 후 Java에서 필터링하는 것이 아니라
     *
     * DB WHERE
     *      ↓
     * 필터링
     *      ↓
     * Slice
     *
     * 순서로 실행되어야 한다.
     */
    @Test
    @DisplayName("리그와 팀 필터가 적용된 결과를 기준으로 페이징한다.")
    void searchPlayer_filterByLeagueAndTeam_paging() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                100L,
                LeagueCode.EPL
        );

        Team chelsea = saveTeam(
                "Chelsea",
                200L,
                LeagueCode.EPL
        );

        Team barcelona = saveTeam(
                "Barcelona",
                300L,
                LeagueCode.LA_LIGA
        );


        // 실제 검색 대상 = 51명
        IntStream.rangeClosed(
                        1,
                        51
                )
                .forEach(i ->
                        savePlayerWithTeam(
                                String.format(
                                        "Arsenal Player%02d",
                                        i
                                ),
                                10_000L + i,
                                arsenal
                        )
                );


        // 같은 EPL이지만 다른 팀
        IntStream.rangeClosed(
                        1,
                        10
                )
                .forEach(i ->
                        savePlayerWithTeam(
                                String.format(
                                        "Chelsea Player%02d",
                                        i
                                ),
                                20_000L + i,
                                chelsea
                        )
                );


        // 다른 리그
        IntStream.rangeClosed(
                        1,
                        10
                )
                .forEach(i ->
                        savePlayerWithTeam(
                                String.format(
                                        "Barcelona Player%02d",
                                        i
                                ),
                                30_000L + i,
                                barcelona
                        )
                );


        // when
        PlayersResponseDto firstPage =
                playerOrchestrationService.getPlayers(
                        0,
                        "",
                        LeagueCode.EPL,
                        arsenal.getId()
                );

        PlayersResponseDto secondPage =
                playerOrchestrationService.getPlayers(
                        1,
                        "",
                        LeagueCode.EPL,
                        arsenal.getId()
                );


        // then
        assertThat(firstPage.getPlayers())
                .hasSize(50);

        assertThat(firstPage.isHasNext())
                .isTrue();

        assertThat(firstPage.isHasPrevious())
                .isFalse();


        assertThat(secondPage.getPlayers())
                .hasSize(1);

        assertThat(secondPage.isHasNext())
                .isFalse();

        assertThat(secondPage.isHasPrevious())
                .isTrue();


        assertThat(firstPage.getPlayers())
                .allSatisfy(player ->
                        assertThat(
                                player.getTeamName()
                        )
                                .isEqualTo(
                                        "Arsenal"
                                )
                );

        assertThat(secondPage.getPlayers())
                .allSatisfy(player ->
                        assertThat(
                                player.getTeamName()
                        )
                                .isEqualTo(
                                        "Arsenal"
                                )
                );
    }


    // ==================================================
    // 현재 소속팀 정책
    // ==================================================

    /*
     * [신규 - 정책 테스트]
     *
     * 선수 목록은 TeamPlayer 기준이다.
     *
     * 따라서 Player 테이블에 존재하더라도
     * 현재 소속팀이 없는 선수는 목록에 노출되지 않는다.
     */
    @Test
    @DisplayName("현재 소속팀이 없는 선수는 선수 검색 결과에 포함하지 않는다.")
    void searchPlayer_withoutTeamPlayer_excluded() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                100L,
                LeagueCode.EPL
        );


        savePlayerWithTeam(
                "Bukayo Saka",
                1L,
                arsenal
        );


        // Player만 존재.
        savePlayer(
                "Free Agent Player",
                2L
        );


        // when
        PlayersResponseDto response =
                playerOrchestrationService.getPlayers(
                        0,
                        "",
                        null,
                        null
                );


        // then
        assertThat(response.getPlayers())
                .hasSize(1);

        assertThat(response.getPlayers())
                .extracting(
                        PlayerItemResponseDto::getPlayerName
                )
                .containsExactly(
                        "Bukayo Saka"
                );

        assertThat(response.getPlayers())
                .extracting(
                        PlayerItemResponseDto::getPlayerName
                )
                .doesNotContain(
                        "Free Agent Player"
                );
    }


    // ==================================================
    // 실패 상황
    // ==================================================

    /*
     * [기존 수정 - 중요]
     *
     * 이 검증은 TxService가 아니라
     * PlayerOrchestrationService의 책임이다.
     *
     * 따라서 Repository 접근 전에 예외가 발생한다.
     */
    @Test
    @DisplayName("페이지 번호가 음수이면 예외가 발생한다.")
    void searchPlayer_negativePage_fail() {

        // given
        int page = -1;


        // when & then
        assertThatThrownBy(() ->
                playerOrchestrationService.getPlayers(
                        page,
                        "Saka",
                        null,
                        null
                )
        )
                .isInstanceOf(
                        InvalidPlayerSearchPageValueException.class
                )
                .hasMessage(
                        "페이지 번호는 0 이상이어야 합니다."
                );
    }


    // ==================================================
    // 경계 상황
    // ==================================================

    /*
     * [기존 수정]
     *
     * keyword trim 역시
     * PlayerOrchestrationService 책임.
     */
    @Test
    @DisplayName("검색어 앞뒤 공백을 제거하고 검색한다.")
    void searchPlayer_trimKeyword() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                100L,
                LeagueCode.EPL
        );

        savePlayerWithTeam(
                "Bukayo Saka",
                1L,
                arsenal
        );

        savePlayerWithTeam(
                "Son Heung-min",
                2L,
                arsenal
        );


        // when
        PlayersResponseDto response =
                playerOrchestrationService.getPlayers(
                        0,
                        "   Saka   ",
                        null,
                        null
                );


        // then
        assertThat(response.getPlayers())
                .extracting(
                        PlayerItemResponseDto::getPlayerName
                )
                .containsExactly(
                        "Bukayo Saka"
                );
    }


    @Test
    @DisplayName("검색 조건에 맞는 선수가 없으면 빈 리스트를 반환한다.")
    void searchPlayer_notFound() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                100L,
                LeagueCode.EPL
        );

        savePlayerWithTeam(
                "Bukayo Saka",
                1L,
                arsenal
        );

        savePlayerWithTeam(
                "Son Heung-min",
                2L,
                arsenal
        );


        // when
        PlayersResponseDto response =
                playerOrchestrationService.getPlayers(
                        0,
                        "Messi",
                        null,
                        null
                );


        // then
        assertThat(response.getPlayers())
                .isEmpty();

        assertThat(response.isHasNext())
                .isFalse();

        assertThat(response.isHasPrevious())
                .isFalse();
    }


    @Test
    @DisplayName("검색어가 공백이면 현재 팀에 소속된 모든 선수를 조회한다.")
    void searchPlayer_blankKeyword_findAll() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                100L,
                LeagueCode.EPL
        );

        savePlayerWithTeam(
                "Son Heung-min",
                1L,
                arsenal
        );

        savePlayerWithTeam(
                "Bukayo Saka",
                2L,
                arsenal
        );

        savePlayerWithTeam(
                "Bruno Fernandes",
                3L,
                arsenal
        );


        // when
        PlayersResponseDto response =
                playerOrchestrationService.getPlayers(
                        0,
                        "   ",
                        null,
                        null
                );


        // then
        assertThat(response.getPlayers())
                .hasSize(3);

        assertThat(response.getPlayers())
                .extracting(
                        PlayerItemResponseDto::getPlayerName
                )
                .containsExactly(
                        "Bruno Fernandes",
                        "Bukayo Saka",
                        "Son Heung-min"
                );
    }


    @Test
    @DisplayName("검색어가 null이면 현재 팀에 소속된 모든 선수를 조회한다.")
    void searchPlayer_nullKeyword_findAll() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                100L,
                LeagueCode.EPL
        );

        savePlayerWithTeam(
                "Son Heung-min",
                1L,
                arsenal
        );

        savePlayerWithTeam(
                "Bukayo Saka",
                2L,
                arsenal
        );


        // when
        PlayersResponseDto response =
                playerOrchestrationService.getPlayers(
                        0,
                        null,
                        null,
                        null
                );


        // then
        assertThat(response.getPlayers())
                .hasSize(2);

        assertThat(response.getPlayers())
                .extracting(
                        PlayerItemResponseDto::getPlayerName
                )
                .containsExactly(
                        "Bukayo Saka",
                        "Son Heung-min"
                );
    }


    @Test
    @DisplayName("존재하지 않는 페이지를 조회하면 빈 리스트를 반환한다.")
    void searchPlayer_pageOutOfRange() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                100L,
                LeagueCode.EPL
        );

        savePlayerWithTeam(
                "Bukayo Saka",
                1L,
                arsenal
        );

        savePlayerWithTeam(
                "Son Heung-min",
                2L,
                arsenal
        );


        // when
        PlayersResponseDto response =
                playerOrchestrationService.getPlayers(
                        100,
                        "",
                        null,
                        null
                );


        // then
        assertThat(response.getPlayers())
                .isEmpty();

        assertThat(response.isHasNext())
                .isFalse();

        assertThat(response.isHasPrevious())
                .isTrue();
    }


    // ==================================================
    // Fixture
    // ==================================================

    /*
     * Player 단독 저장.
     *
     * TeamPlayer가 없는 선수 정책 테스트에서 사용한다.
     */
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


    /*
     * Team 생성.
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


    /*
     * 현재 소속팀을 가진 선수 생성.
     *
     * Player
     *      +
     * Team
     *      +
     * TeamPlayer
     */
    private Player savePlayerWithTeam(
            String playerName,
            Long apiFootballId,
            Team team
    ) {

        Player player =
                savePlayer(
                        playerName,
                        apiFootballId
                );

        teamPlayerRepository.save(
                TeamPlayer.of(
                        team,
                        player
                )
        );

        return player;
    }
}