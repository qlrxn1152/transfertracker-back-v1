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
@Import(PlayerServiceImpl.class)
@ActiveProfiles("test")
class PlayersSearchTest {

    @Autowired
    PlayerService playerService;

    @Autowired
    PlayerRepository playerRepository;

    // [신규]
    // 선수 검색의 기준이 TeamPlayer가 되었기 때문에 필요.
    @Autowired
    TeamRepository teamRepository;

    // [신규]
    @Autowired
    TeamPlayerRepository teamPlayerRepository;


    // ==================================================
    // 정상 상황
    // ==================================================

    /*
     * [기존 수정]
     *
     * 기존에는 Player만 저장하면 검색 가능했지만,
     * 현재 정책은 "현재 팀에 소속된 선수만 검색한다."
     *
     * 따라서 Player + Team + TeamPlayer를 함께 생성한다.
     *
     * leagueCode == null
     * → 리그 필터 없이 기존 이름 검색 동작 검증.
     */
    @Test
    @DisplayName("선수 이름의 일부를 이용해서 검색할 수 있다.")
    void searchPlayer_byPartialName() {

        // given
        Team team = saveTeam(
                "Test Team",
                100L,
                LeagueCode.EPL
        );

        savePlayerWithTeam(
                "Son Heung-min",
                1L,
                team
        );

        savePlayerWithTeam(
                "Bukayo Saka",
                2L,
                team
        );

        savePlayerWithTeam(
                "Bruno Fernandes",
                3L,
                team
        );

        // when
        PlayersResponseDto response =
                playerService.getPlayers(
                        0,
                        "Heung",
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
     *
     * 기존 ignore case 정책은 그대로.
     * leagueCode 인자만 추가되고,
     * 선수는 반드시 TeamPlayer를 가진 상태로 생성.
     */
    @Test
    @DisplayName("선수 이름 검색은 대소문자를 구분하지 않는다.")
    void searchPlayer_ignoreCase() {

        // given
        Team team = saveTeam(
                "Test Team",
                100L,
                LeagueCode.EPL
        );

        savePlayerWithTeam(
                "Bukayo Saka",
                1L,
                team
        );

        savePlayerWithTeam(
                "Son Heung-min",
                2L,
                team
        );

        // when
        PlayersResponseDto response =
                playerService.getPlayers(
                        0,
                        "sAkA",
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
     * 현재 정렬 기준:
     *
     * player.playerName ASC
     * player.id ASC
     *
     * 조회 대상이 TeamPlayer가 되었기 때문에
     * Service Pageable의 property path도 player.playerName을 사용.
     */
    @Test
    @DisplayName("선수 목록은 이름 오름차순으로 조회된다.")
    void searchPlayer_sortByName() {

        // given
        Team team = saveTeam(
                "Test Team",
                100L,
                LeagueCode.EPL
        );

        savePlayerWithTeam(
                "Son Heung-min",
                1L,
                team
        );

        savePlayerWithTeam(
                "Bruno Fernandes",
                2L,
                team
        );

        savePlayerWithTeam(
                "Bukayo Saka",
                3L,
                team
        );

        // when
        PlayersResponseDto response =
                playerService.getPlayers(
                        0,
                        "",
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
     * [기존 수정]
     *
     * 기존 Slice paging 정책 그대로.
     *
     * 다만 51명의 Player만 만드는 것이 아니라
     * 51명의 Player가 TeamPlayer에 연결되어 있어야 한다.
     */
    @Test
    @DisplayName("검색 결과가 50개를 초과하면 Slice 페이징 정보가 정상적으로 반환된다.")
    void searchPlayer_paging() {

        // given
        Team team = saveTeam(
                "Test Team",
                100L,
                LeagueCode.EPL
        );

        IntStream.rangeClosed(1, 51)
                .forEach(i ->
                        savePlayerWithTeam(
                                String.format("Player%02d", i),
                                (long) i,
                                team
                        )
                );

        // when
        PlayersResponseDto firstPage =
                playerService.getPlayers(
                        0,
                        "player",
                        null
                );

        PlayersResponseDto secondPage =
                playerService.getPlayers(
                        1,
                        "player",
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


    // ==================================================
    // [신규] 리그 필터
    // ==================================================

    /*
     * [신규]
     *
     * TeamPlayer.team.leagueCode가
     * 요청한 leagueCode와 같을 때만 조회한다.
     */
    @Test
    @DisplayName("선수의 현재 소속팀이 선택한 리그에 속하면 조회한다.")
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
                        LeagueCode.EPL
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
     * 선택한 리그와 다른 팀에 소속된 선수는
     * 검색 결과에서 제외되어야 한다.
     */
    @Test
    @DisplayName("현재 소속팀이 선택한 리그가 아니면 조회하지 않는다.")
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
                        LeagueCode.EPL
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
     * [신규]
     *
     * Repository 조건:
     *
     * (:leagueCode is null OR t.leagueCode = :leagueCode)
     *
     * leagueCode == null이면
     * 리그 필터가 적용되지 않는다.
     *
     * 단, 현재 조회 기준은 TeamPlayer이므로
     * "팀에 소속된 선수들 전체"를 의미한다.
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
                        null
                );

        // then
        assertThat(response.getPlayers())
                .hasSize(2);

        assertThat(response.getPlayers())
                .extracting(PlayerItemResponseDto::getPlayerName)
                .containsExactly(
                        "Bukayo Saka",
                        "Pedri"
                );
    }


    /*
     * [신규 - 중요]
     *
     * Repository JPQL:
     *
     * (league 조건)
     * AND
     * (keyword 조건)
     *
     * 두 조건을 모두 만족하는 선수만 조회되어야 한다.
     */
    @Test
    @DisplayName("리그와 선수 이름 검색 조건을 모두 만족하는 선수만 조회한다.")
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

        /*
         * Saka + EPL
         * → 리그 O
         * → keyword O
         * → 조회 대상
         */
        savePlayerWithTeam(
                "Bukayo Saka",
                1L,
                arsenal
        );

        /*
         * Bruno + EPL
         * → 리그 O
         * → keyword X
         * → 제외
         */
        savePlayerWithTeam(
                "Bruno Fernandes",
                2L,
                manUnited
        );

        /*
         * Saka Test + LA_LIGA
         * → keyword O
         * → 리그 X
         * → 제외
         */
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
                        LeagueCode.EPL
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
     * [신규 - 중요]
     *
     * 리그 필터는 페이지 조회 후 Java에서 거르는 게 아니라
     * DB WHERE 절에서 먼저 적용되어야 한다.
     *
     * EPL 선수 51명
     * LA_LIGA 선수 10명
     *
     * EPL 검색 결과:
     *
     * page 0 → 50명
     * page 1 → 1명
     *
     * 이어야 한다.
     */
    @Test
    @DisplayName("리그 필터가 적용된 결과를 기준으로 페이징한다.")
    void searchPlayer_filterByLeague_paging() {

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


        // EPL 51명
        IntStream.rangeClosed(1, 51)
                .forEach(i ->
                        savePlayerWithTeam(
                                String.format("EPL Player%02d", i),
                                10_000L + i,
                                arsenal
                        )
                );


        // LA_LIGA 10명
        IntStream.rangeClosed(1, 10)
                .forEach(i ->
                        savePlayerWithTeam(
                                String.format("LaLiga Player%02d", i),
                                20_000L + i,
                                barcelona
                        )
                );


        // when
        PlayersResponseDto firstPage =
                playerService.getPlayers(
                        0,
                        "",
                        LeagueCode.EPL
                );

        PlayersResponseDto secondPage =
                playerService.getPlayers(
                        1,
                        "",
                        LeagueCode.EPL
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


        /*
         * 다른 리그 선수가 섞이지 않았는지도 확인.
         */
        assertThat(firstPage.getPlayers())
                .extracting(PlayerItemResponseDto::getPlayerName)
                .allMatch(name ->
                        name.startsWith("EPL Player")
                );

        assertThat(secondPage.getPlayers())
                .extracting(PlayerItemResponseDto::getPlayerName)
                .allMatch(name ->
                        name.startsWith("EPL Player")
                );
    }


    /*
     * [신규 - 정책 테스트]
     *
     * 이번에 정한 정책:
     *
     * "현재 팀에 소속되어 있는 선수만 선수 검색 결과에 노출한다."
     *
     * 따라서 Player 테이블에는 존재하지만
     * TeamPlayer가 없는 선수는 조회되면 안 된다.
     */
    @Test
    @DisplayName("현재 소속팀이 없는 선수는 선수 검색 결과에 포함하지 않는다.")
    void searchPlayer_withoutTeamPlayer_excluded() {

        // given

        /*
         * TeamPlayer가 있는 선수
         */
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
         * Player에만 존재.
         * TeamPlayer는 생성하지 않는다.
         */
        savePlayer(
                "Free Agent Player",
                2L
        );


        // when
        PlayersResponseDto response =
                playerService.getPlayers(
                        0,
                        "",
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


    /*
     * [신규]
     *
     * fetch join으로 가져온 Team이
     * PlayerItemResponseDto에도 정상적으로 매핑되는지 확인한다.
     */
    @Test
    @DisplayName("검색한 선수의 현재 소속팀 정보가 함께 반환된다.")
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
    // 예외 상황
    // ==================================================

    /*
     * [기존 수정]
     *
     * page 정책은 그대로.
     * 새 leagueCode 파라미터만 추가.
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
        Team team = saveTeam(
                "Test Team",
                100L,
                LeagueCode.EPL
        );

        savePlayerWithTeam(
                "Bukayo Saka",
                1L,
                team
        );

        savePlayerWithTeam(
                "Son Heung-min",
                2L,
                team
        );

        // when
        PlayersResponseDto response =
                playerService.getPlayers(
                        0,
                        "   Saka   ",
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
        Team team = saveTeam(
                "Test Team",
                100L,
                LeagueCode.EPL
        );

        savePlayerWithTeam(
                "Bukayo Saka",
                1L,
                team
        );

        savePlayerWithTeam(
                "Son Heung-min",
                2L,
                team
        );

        // when
        PlayersResponseDto response =
                playerService.getPlayers(
                        0,
                        "Messi",
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
     * "모든 선수"라는 의미가 조금 바뀌었다.
     *
     * 기존:
     * Player 전체
     *
     * 현재:
     * TeamPlayer가 존재하는 선수 전체
     */
    @Test
    @DisplayName("검색어가 공백이면 현재 팀에 소속된 모든 선수를 조회한다.")
    void searchPlayer_blankKeyword_findAll() {

        // given
        Team team = saveTeam(
                "Test Team",
                100L,
                LeagueCode.EPL
        );

        savePlayerWithTeam(
                "Son Heung-min",
                1L,
                team
        );

        savePlayerWithTeam(
                "Bukayo Saka",
                2L,
                team
        );

        savePlayerWithTeam(
                "Bruno Fernandes",
                3L,
                team
        );

        // when
        PlayersResponseDto response =
                playerService.getPlayers(
                        0,
                        "   ",
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
        Team team = saveTeam(
                "Test Team",
                100L,
                LeagueCode.EPL
        );

        savePlayerWithTeam(
                "Son Heung-min",
                1L,
                team
        );

        savePlayerWithTeam(
                "Bukayo Saka",
                2L,
                team
        );

        // when
        PlayersResponseDto response =
                playerService.getPlayers(
                        0,
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
        Team team = saveTeam(
                "Test Team",
                100L,
                LeagueCode.EPL
        );

        savePlayerWithTeam(
                "Bukayo Saka",
                1L,
                team
        );

        savePlayerWithTeam(
                "Son Heung-min",
                2L,
                team
        );

        // when
        PlayersResponseDto response =
                playerService.getPlayers(
                        100,
                        "",
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
     * TeamPlayer가 없는 Player 정책을 테스트할 때도 필요하므로
     * 기존 savePlayer Fixture는 그대로 유지한다.
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
     *
     * 리그 필터 및 현재 소속팀 생성용.
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
     * 현재 선수 검색 정책에서는
     * Player만 저장하면 검색 대상이 아니다.
     *
     * 따라서 대부분의 테스트에서
     *
     * Player 저장
     * +
     * TeamPlayer 저장
     *
     * 을 한 번에 처리한다.
     */
    private Player savePlayerWithTeam(
            String playerName,
            Long apiFootballId,
            Team team
    ) {

        Player player = savePlayer(
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