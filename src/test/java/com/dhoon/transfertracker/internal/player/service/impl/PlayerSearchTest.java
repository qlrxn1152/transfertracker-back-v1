package com.dhoon.transfertracker.internal.player.service.impl;

import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.dto.response.PlayerItemResponseDto;
import com.dhoon.transfertracker.internal.player.dto.response.PlayersResponseDto;
import com.dhoon.transfertracker.internal.player.exception.InvalidPlayerSearchPageValueException;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.player.service.PlayerService;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.teamplayer.domain.TeamPlayer;
import com.dhoon.transfertracker.internal.teamplayer.repository.TeamPlayerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@DataJpaTest
@Import(PlayerOrchestrationService.class)
@ActiveProfiles("test")
class PlayersSearchTest {

    @Autowired
    PlayerService playerService;

    @Autowired
    PlayerRepository playerRepository;

    // [신규]
    // 현재 선수 목록은 TeamPlayer 기준으로 조회하므로
    // Team / TeamPlayer Fixture가 필요하다.
    @Autowired
    TeamRepository teamRepository;

    @Autowired
    TeamPlayerRepository teamPlayerRepository;


    // ==================================================
    // 정상 상황
    // ==================================================

    /*
     * [기존 수정]
     *
     * 기존에는 Player만 저장했지만,
     * 현재는 TeamPlayer가 존재하는 선수만 검색 대상이다.
     *
     * leagueCode = null
     * teamId = null
     *
     * → 리그 / 팀 필터 없이 기존 이름 검색 동작 검증.
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
                playerService.getPlayers(
                        0,
                        "Heung",
                        null,
                        null
                );


        // then
        assertThat(response.getPlayers())
                .hasSize(1);

        assertThat(response.getPlayers())
                .extracting(PlayerItemResponseDto::getPlayerName)
                .containsExactly("Son Heung-min");
    }


    /*
     * [기존 수정]
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
                playerService.getPlayers(
                        0,
                        "sAkA",
                        null,
                        null
                );


        // then
        assertThat(response.getPlayers())
                .extracting(PlayerItemResponseDto::getPlayerName)
                .containsExactly("Bukayo Saka");
    }


    /*
     * [기존 수정]
     *
     * 조회 대상이 TeamPlayer가 되었으므로
     * Service의 Sort는:
     *
     * player.playerName ASC
     * player.id ASC
     *
     * 를 사용한다.
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
                playerService.getPlayers(
                        0,
                        "",
                        null,
                        null
                );


        // then
        assertThat(response.getPlayers())
                .extracting(PlayerItemResponseDto::getPlayerName)
                .containsExactly(
                        "Bruno Fernandes",
                        "Bukayo Saka",
                        "Son Heung-min"
                );
    }


    /*
     * [신규]
     *
     * playerName이 동일하면 player.id ASC가
     * 두 번째 정렬 기준이 된다.
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

        Player firstPlayer = savePlayerWithTeam(
                "Same Player",
                1L,
                arsenal
        );

        Player secondPlayer = savePlayerWithTeam(
                "Same Player",
                2L,
                arsenal
        );


        // when
        PlayersResponseDto response =
                playerService.getPlayers(
                        0,
                        "Same Player",
                        null,
                        null
                );


        // then
        assertThat(response.getPlayers())
                .extracting(PlayerItemResponseDto::getPlayerId)
                .containsExactly(
                        firstPlayer.getId(),
                        secondPlayer.getId()
                );
    }


    /*
     * [기존 수정]
     *
     * 51명의 Player뿐 아니라
     * 51개의 TeamPlayer도 존재해야 한다.
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

        IntStream.rangeClosed(1, 51)
                .forEach(i ->
                        savePlayerWithTeam(
                                String.format("Player%02d", i),
                                (long) i,
                                arsenal
                        )
                );


        // when
        PlayersResponseDto firstPage =
                playerService.getPlayers(
                        0,
                        "player",
                        null,
                        null
                );

        PlayersResponseDto secondPage =
                playerService.getPlayers(
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
     * [신규]
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

        IntStream.rangeClosed(1, 50)
                .forEach(i ->
                        savePlayerWithTeam(
                                String.format("Player%02d", i),
                                (long) i,
                                arsenal
                        )
                );


        // when
        PlayersResponseDto response =
                playerService.getPlayers(
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
     * [신규]
     *
     * fetch join으로 Player / Team을 함께 가져온 뒤
     * DTO에 정상적으로 매핑되는지 검증.
     */
    @Test
    @DisplayName("선수 검색 결과에 현재 소속팀 정보가 함께 포함된다.")
    void searchPlayer_mappingTeam() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                100L,
                LeagueCode.EPL
        );

        arsenal.assignKoTeamName("아스널");

        Player saka = savePlayerWithTeam(
                "Bukayo Saka",
                1L,
                arsenal
        );


        // when
        PlayersResponseDto response =
                playerService.getPlayers(
                        0,
                        "Saka",
                        null,
                        null
                );


        // then
        PlayerItemResponseDto player =
                response.getPlayers().get(0);

        assertThat(player.getPlayerId())
                .isEqualTo(saka.getId());

        assertThat(player.getPlayerName())
                .isEqualTo("Bukayo Saka");

        assertThat(player.getTeamName())
                .isEqualTo("Arsenal");

        assertThat(player.getTeamNameKo())
                .isEqualTo("아스널");

        assertThat(player.getPhotoUrl())
                .isEqualTo(
                        "https://media.api-sports.io/football/players/1.png"
                );
    }


    // ==================================================
    // [신규] 리그 필터
    // ==================================================

    /*
     * [신규]
     *
     * t.leagueCode = :leagueCode
     *
     * 조건 검증.
     */
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
                playerService.getPlayers(
                        0,
                        "",
                        LeagueCode.EPL,
                        null
                );


        // then
        assertThat(response.getPlayers())
                .hasSize(1);

        assertThat(response.getPlayers())
                .extracting(PlayerItemResponseDto::getPlayerName)
                .containsExactly("Bukayo Saka");
    }


    /*
     * [신규]
     */
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
                playerService.getPlayers(
                        0,
                        "",
                        LeagueCode.EPL,
                        null
                );


        // then
        assertThat(response.getPlayers())
                .isEmpty();
    }


    /*
     * [신규]
     *
     * :leagueCode is null
     *
     * → 리그 조건을 사용하지 않는다.
     *
     * 단, 현재 정책상 TeamPlayer가 존재하는 선수만 대상.
     */
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
                playerService.getPlayers(
                        0,
                        "",
                        null,
                        null
                );


        // then
        assertThat(response.getPlayers())
                .extracting(PlayerItemResponseDto::getPlayerName)
                .containsExactly(
                        "Bukayo Saka",
                        "Pedri"
                );
    }


    /*
     * [신규]
     *
     * leagueCode
     * AND
     * keyWord
     *
     * 두 조건을 모두 만족해야 한다.
     */
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
                playerService.getPlayers(
                        0,
                        "Saka",
                        LeagueCode.EPL,
                        null
                );


        // then
        assertThat(response.getPlayers())
                .hasSize(1);

        assertThat(response.getPlayers())
                .extracting(PlayerItemResponseDto::getPlayerName)
                .containsExactly("Bukayo Saka");
    }


    // ==================================================
    // [신규] 팀 필터
    // ==================================================

    /*
     * [신규]
     *
     * t.id = :teamId
     *
     * 조건을 검증한다.
     */
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
                playerService.getPlayers(
                        0,
                        "",
                        null,
                        arsenal.getId()
                );


        // then
        assertThat(response.getPlayers())
                .hasSize(1);

        PlayerItemResponseDto player =
                response.getPlayers().get(0);

        assertThat(player.getPlayerName())
                .isEqualTo("Bukayo Saka");

        assertThat(player.getTeamName())
                .isEqualTo("Arsenal");
    }


    /*
     * [신규]
     *
     * teamId
     * AND
     * keyWord
     */
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
                playerService.getPlayers(
                        0,
                        "Saka",
                        null,
                        arsenal.getId()
                );


        // then
        assertThat(response.getPlayers())
                .hasSize(1);

        assertThat(response.getPlayers())
                .extracting(PlayerItemResponseDto::getPlayerName)
                .containsExactly("Bukayo Saka");
    }


    /*
     * [신규]
     *
     * leagueCode
     * AND
     * teamId
     *
     * 둘 다 만족해야 한다.
     */
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
                playerService.getPlayers(
                        0,
                        "",
                        LeagueCode.EPL,
                        arsenal.getId()
                );


        // then
        assertThat(response.getPlayers())
                .hasSize(1);

        assertThat(response.getPlayers())
                .extracting(PlayerItemResponseDto::getPlayerName)
                .containsExactly("Bukayo Saka");
    }


    /*
     * [신규 - 중요]
     *
     * leagueCode / teamId 조합이 서로 맞지 않으면
     * 한 조건을 무시하는 것이 아니라
     * AND이므로 빈 결과를 반환한다.
     *
     * 예:
     *
     * leagueCode = LA_LIGA
     * teamId = Arsenal(EPL)
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
                playerService.getPlayers(
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
     * AND
     * teamId
     * AND
     * keyWord
     *
     * 세 조건이 모두 적용되는지 검증한다.
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
                playerService.getPlayers(
                        0,
                        "Saka",
                        LeagueCode.EPL,
                        arsenal.getId()
                );


        // then
        assertThat(response.getPlayers())
                .hasSize(1);

        PlayerItemResponseDto player =
                response.getPlayers().get(0);

        assertThat(player.getPlayerName())
                .isEqualTo("Bukayo Saka");

        assertThat(player.getTeamName())
                .isEqualTo("Arsenal");
    }


    /*
     * [신규]
     *
     * 존재하지 않는 teamId를 필터로 전달해도
     * 예외가 아니라 빈 검색 결과를 반환하는 정책.
     *
     * 현재 Repository 검색 조건 방식이라면
     * 자연스럽게 0건이 반환된다.
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
                playerService.getPlayers(
                        0,
                        "",
                        null,
                        999999L
                );


        // then
        assertThat(response.getPlayers())
                .isEmpty();
    }


    // ==================================================
    // [신규] 필터 + 페이징
    // ==================================================

    /*
     * [신규 - 중요]
     *
     * 필터링을 Java에서 페이지 조회 후 하는 것이 아니라
     * DB WHERE 절에서 먼저 처리한 뒤
     * Slice 페이징하는지 검증한다.
     *
     * Arsenal(EPL) → 51명
     * Chelsea(EPL) → 10명
     * Barcelona(LA_LIGA) → 10명
     *
     * EPL + Arsenal 필터 결과는 정확히 51명이어야 하므로:
     *
     * page 0 = 50명
     * page 1 = 1명
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


        // 검색 대상 51명
        IntStream.rangeClosed(1, 51) // 아스날에 51명.
                .forEach(i ->
                        savePlayerWithTeam(
                                String.format("Arsenal Player%02d", i),
                                10_000L + i,
                                arsenal
                        )
                );


        // 같은 EPL이지만 다른 팀 → 제외되어야 함.
        IntStream.rangeClosed(1, 10) // 첼시에 10명
                .forEach(i ->
                        savePlayerWithTeam(
                                String.format("Chelsea Player%02d", i),
                                20_000L + i,
                                chelsea
                        )
                );


        // 다른 리그 → 제외되어야 함.
        IntStream.rangeClosed(1, 10) // 바르샤10명
                .forEach(i ->
                        savePlayerWithTeam(
                                String.format("Barcelona Player%02d", i),
                                30_000L + i,
                                barcelona
                        )
                );


        // when
        PlayersResponseDto firstPage =
                playerService.getPlayers(
                        0,
                        "",
                        LeagueCode.EPL,
                        arsenal.getId()
                );

        PlayersResponseDto secondPage =
                playerService.getPlayers(
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
                        assertThat(player.getTeamName())
                                .isEqualTo("Arsenal")
                );

        assertThat(secondPage.getPlayers())
                .allSatisfy(player ->
                        assertThat(player.getTeamName())
                                .isEqualTo("Arsenal")
                );
    }


    // ==================================================
    // [신규] 현재 소속팀 정책
    // ==================================================

    /*
     * [신규 - 정책 테스트]
     *
     * 현재 정책:
     *
     * "현재 TeamPlayer가 존재하는 선수만
     * 선수 목록 검색 결과에 노출한다."
     *
     * Player 테이블에만 존재하는 FA / 무소속 선수는
     * 목록에서 제외한다.
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


        // 정상 소속 선수
        savePlayerWithTeam(
                "Bukayo Saka",
                1L,
                arsenal
        );


        // Player만 존재.
        // TeamPlayer는 생성하지 않음.
        savePlayer(
                "Free Agent Player",
                2L
        );


        // when
        PlayersResponseDto response =
                playerService.getPlayers(
                        0,
                        "",
                        null,
                        null
                );


        // then
        assertThat(response.getPlayers())
                .hasSize(1);

        assertThat(response.getPlayers())
                .extracting(PlayerItemResponseDto::getPlayerName)
                .containsExactly("Bukayo Saka");

        assertThat(response.getPlayers())
                .extracting(PlayerItemResponseDto::getPlayerName)
                .doesNotContain("Free Agent Player");
    }


    // ==================================================
    // 예외 상황
    // ==================================================

    /*
     * [기존 수정]
     *
     * leagueCode / teamId 인자만 추가.
     */
    @Test
    @DisplayName("페이지 번호가 음수이면 예외가 발생한다.")
    void searchPlayer_negativePage_fail() {

        // given
        int page = -1;


        // when & then
        assertThatThrownBy(() ->
                playerService.getPlayers(
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
                playerService.getPlayers(
                        0,
                        "   Saka   ",
                        null,
                        null
                );


        // then
        assertThat(response.getPlayers())
                .extracting(PlayerItemResponseDto::getPlayerName)
                .containsExactly("Bukayo Saka");
    }


    /*
     * [기존 수정]
     */
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
                playerService.getPlayers(
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


    /*
     * [기존 수정]
     *
     * 여기서 "모든 선수"는:
     *
     * Player 전체가 아니라
     * TeamPlayer가 존재하는 모든 선수
     *
     * 를 의미한다.
     */
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
                playerService.getPlayers(
                        0,
                        "   ",
                        null,
                        null
                );


        // then
        assertThat(response.getPlayers())
                .hasSize(3);

        assertThat(response.getPlayers())
                .extracting(PlayerItemResponseDto::getPlayerName)
                .containsExactly(
                        "Bruno Fernandes",
                        "Bukayo Saka",
                        "Son Heung-min"
                );
    }


    /*
     * [기존 수정]
     */
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
                playerService.getPlayers(
                        0,
                        null,
                        null,
                        null
                );


        // then
        assertThat(response.getPlayers())
                .hasSize(2);

        assertThat(response.getPlayers())
                .extracting(PlayerItemResponseDto::getPlayerName)
                .containsExactly(
                        "Bukayo Saka",
                        "Son Heung-min"
                );
    }


    /*
     * [기존 수정]
     */
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
                playerService.getPlayers(
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
     * [기존 유지]
     *
     * TeamPlayer가 없는 선수 정책을 테스트하기 위해
     * Player 단독 저장 Fixture도 유지한다.
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
     * [신규 Fixture]
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
     * [신규 Fixture]
     *
     * 현재 선수 목록의 기본 단위:
     *
     * Player
     * +
     * TeamPlayer
     * +
     * Team
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