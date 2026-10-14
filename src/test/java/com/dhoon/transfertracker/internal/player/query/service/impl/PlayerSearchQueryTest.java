package com.dhoon.transfertracker.internal.player.query.service.impl;

import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.exception.InvalidPlayerSearchPageValueException;
import com.dhoon.transfertracker.internal.player.query.dto.response.PlayerItemResponseDto;
import com.dhoon.transfertracker.internal.player.query.dto.response.PlayersResponseDto;
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
        PlayerQueryOrchestrationService.class,
        PlayerQueryTxService.class
})
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PlayerSearchQueryTest {


    @Autowired
    PlayerQueryOrchestrationService playerQueryOrchestrationService;

    @Autowired
    PlayerRepository playerRepository;

    @Autowired
    TeamRepository teamRepository;

    @Autowired
    TeamPlayerRepository teamPlayerRepository;


    @AfterEach
    void clearDatabase() {

        teamPlayerRepository.deleteAll();

        playerRepository.deleteAll();
        teamRepository.deleteAll();
    }


    // ==================================================
    // 이름 검색
    // ==================================================

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
                playerQueryOrchestrationService.getPlayers(
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


        // when
        PlayersResponseDto response =
                playerQueryOrchestrationService.getPlayers(
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


    @Test
    @DisplayName("선수 한글 이름으로도 검색할 수 있다.")
    void searchPlayer_byKoreanName() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                100L,
                LeagueCode.EPL
        );


        Player saka =
                Player.of(
                        "Bukayo Saka",
                        1L
                );

        saka.assignKoPlayerName(
                "부카요 사카"
        );

        saka =
                playerRepository.save(
                        saka
                );


        teamPlayerRepository.save(
                TeamPlayer.of(
                        arsenal,
                        saka
                )
        );


        // when
        PlayersResponseDto response =
                playerQueryOrchestrationService.getPlayers(
                        0,
                        "사카",
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
                        "부카요 사카"
                );
    }


    // ==================================================
    // Keyword 정규화
    // ==================================================

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
                playerQueryOrchestrationService.getPlayers(
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
                playerQueryOrchestrationService.getPlayers(
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
                playerQueryOrchestrationService.getPlayers(
                        0,
                        null,
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
                        "Son Heung-min"
                );
    }


    // ==================================================
    // 정렬
    // ==================================================

    @Test
    @DisplayName("선수 목록은 영문 이름 오름차순으로 조회된다.")
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
                playerQueryOrchestrationService.getPlayers(
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


    @Test
    @DisplayName("선수 영문 이름이 같으면 Player ID 오름차순으로 조회된다.")
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
                playerQueryOrchestrationService.getPlayers(
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


    // ==================================================
    // DTO Mapping
    // ==================================================

    @Test
    @DisplayName("선수 검색 결과에는 현재 소속팀과 선수 사진 정보가 포함된다.")
    void searchPlayer_dtoMapping() {

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
                playerQueryOrchestrationService.getPlayers(
                        0,
                        "Saka",
                        null,
                        null
                );


        // then
        assertThat(response.getPlayers())
                .hasSize(1);


        PlayerItemResponseDto player =
                response.getPlayers()
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
                        "아스널"
                );

        assertThat(player.getPhotoUrl())
                .isEqualTo(
                        "https://media.api-sports.io/football/players/1.png"
                );
    }


    // ==================================================
    // League 필터
    // ==================================================

    @Test
    @DisplayName("선택한 리그에 현재 소속된 선수만 조회한다.")
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
                playerQueryOrchestrationService.getPlayers(
                        0,
                        "",
                        LeagueCode.EPL,
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


        savePlayerWithTeam(
                "Bukayo Saka",
                1L,
                arsenal
        );

        savePlayerWithTeam(
                "Bruno Fernandes",
                2L,
                manUnited
        );

        savePlayerWithTeam(
                "Saka Test",
                3L,
                barcelona
        );


        // when
        PlayersResponseDto response =
                playerQueryOrchestrationService.getPlayers(
                        0,
                        "Saka",
                        LeagueCode.EPL,
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


    // ==================================================
    // Team 필터
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
                playerQueryOrchestrationService.getPlayers(
                        0,
                        "",
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

        assertThat(response.getPlayers())
                .extracting(
                        PlayerItemResponseDto::getTeamName
                )
                .containsExactly(
                        "Arsenal"
                );
    }


    @Test
    @DisplayName("존재하지 않는 팀 ID로 필터링하면 빈 목록을 반환한다.")
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
                playerQueryOrchestrationService.getPlayers(
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

        assertThat(response.isHasPrevious())
                .isFalse();
    }


    // ==================================================
    // 복합 필터
    // ==================================================

    @Test
    @DisplayName("선택한 리그와 팀을 모두 만족하는 선수만 조회한다.")
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
                playerQueryOrchestrationService.getPlayers(
                        0,
                        "",
                        LeagueCode.EPL,
                        arsenal.getId()
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
    @DisplayName("선택한 리그와 팀이 서로 일치하지 않으면 빈 목록을 반환한다.")
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
                playerQueryOrchestrationService.getPlayers(
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


    @Test
    @DisplayName("리그, 팀, 이름 조건을 모두 만족하는 선수만 조회한다.")
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


        // 모든 조건 만족
        savePlayerWithTeam(
                "Bukayo Saka",
                1L,
                arsenal
        );

        // Team O / Keyword X
        savePlayerWithTeam(
                "Martin Odegaard",
                2L,
                arsenal
        );

        // League O / Keyword O / Team X
        savePlayerWithTeam(
                "Saka Chelsea",
                3L,
                chelsea
        );

        // Keyword O / League X / Team X
        savePlayerWithTeam(
                "Saka Barcelona",
                4L,
                barcelona
        );


        // when
        PlayersResponseDto response =
                playerQueryOrchestrationService.getPlayers(
                        0,
                        "Saka",
                        LeagueCode.EPL,
                        arsenal.getId()
                );


        // then
        assertThat(response.getPlayers())
                .hasSize(1);

        assertThat(
                response.getPlayers()
                        .get(0)
                        .getPlayerName()
        )
                .isEqualTo(
                        "Bukayo Saka"
                );
    }


    // ==================================================
    // 현재 소속팀 정책
    // ==================================================

    @Test
    @DisplayName("현재 소속팀이 없는 선수는 선수 목록에 포함하지 않는다.")
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


        /*
         * Player에는 존재하지만
         * TeamPlayer가 없다.
         */
        savePlayer(
                "Free Agent Player",
                2L
        );


        // when
        PlayersResponseDto response =
                playerQueryOrchestrationService.getPlayers(
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
    // Slice Paging
    // ==================================================

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
                playerQueryOrchestrationService.getPlayers(
                        0,
                        "player",
                        null,
                        null
                );

        PlayersResponseDto secondPage =
                playerQueryOrchestrationService.getPlayers(
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
                playerQueryOrchestrationService.getPlayers(
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


        // 실제 조회 대상 51명
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


        // 같은 리그 / 다른 팀
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
                playerQueryOrchestrationService.getPlayers(
                        0,
                        "",
                        LeagueCode.EPL,
                        arsenal.getId()
                );

        PlayersResponseDto secondPage =
                playerQueryOrchestrationService.getPlayers(
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
                                .isEqualTo(
                                        "Arsenal"
                                )
                );

        assertThat(secondPage.getPlayers())
                .allSatisfy(player ->
                        assertThat(player.getTeamName())
                                .isEqualTo(
                                        "Arsenal"
                                )
                );
    }


    @Test
    @DisplayName("존재하지 않는 페이지를 조회하면 빈 목록을 반환한다.")
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
                playerQueryOrchestrationService.getPlayers(
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
    // 빈 결과
    // ==================================================

    @Test
    @DisplayName("검색 조건에 맞는 선수가 없으면 빈 목록을 반환한다.")
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


        // when
        PlayersResponseDto response =
                playerQueryOrchestrationService.getPlayers(
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


    // ==================================================
    // 실패
    // ==================================================

    @Test
    @DisplayName("페이지 번호가 음수이면 예외가 발생한다.")
    void searchPlayer_negativePage_fail() {

        // when & then
        assertThatThrownBy(() ->
                playerQueryOrchestrationService.getPlayers(
                        -1,
                        "Saka",
                        null,
                        null
                )
        )
                .isInstanceOf(
                        InvalidPlayerSearchPageValueException.class
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