package com.dhoon.transfertracker.internal.player.service.impl;

import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.dto.response.PlayerItemResponseDto;
import com.dhoon.transfertracker.internal.player.dto.response.PlayersResponseDto;
import com.dhoon.transfertracker.internal.player.exception.InvalidPlayerSearchPageValueException;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.player.service.PlayerService;
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


    // ==================================================
    // 정상 상황
    // ==================================================

    @Test
    @DisplayName("선수 이름의 일부를 이용해서 검색할 수 있다.")
    void searchPlayer_byPartialName() {

        // given
        savePlayer("Son Heung-min", 1L);
        savePlayer("Bukayo Saka", 2L);
        savePlayer("Bruno Fernandes", 3L);

        // when
        PlayersResponseDto response =
                playerService.getPlayers(0, "Heung");

        // then
        assertThat(response.getPlayers())
                .hasSize(1);

        assertThat(response.getPlayers())
                .extracting(PlayerItemResponseDto::getPlayerName)
                .containsExactly("Son Heung-min");
    }


    @Test
    @DisplayName("선수 이름 검색은 대소문자를 구분하지 않는다.")
    void searchPlayer_ignoreCase() {

        // given
        savePlayer("Bukayo Saka", 1L);
        savePlayer("Son Heung-min", 2L);

        // when
        PlayersResponseDto response =
                playerService.getPlayers(0, "sAkA");

        // then
        assertThat(response.getPlayers())
                .extracting(PlayerItemResponseDto::getPlayerName)
                .containsExactly("Bukayo Saka");
    }


    @Test
    @DisplayName("선수 목록은 이름 오름차순으로 조회된다.")
    void searchPlayer_sortByName() {

        // given
        savePlayer("Son Heung-min", 1L);
        savePlayer("Bruno Fernandes", 2L);
        savePlayer("Bukayo Saka", 3L);

        // when
        PlayersResponseDto response =
                playerService.getPlayers(0, "");

        // then
        assertThat(response.getPlayers())
                .extracting(PlayerItemResponseDto::getPlayerName)
                .containsExactly(
                        "Bruno Fernandes",
                        "Bukayo Saka",
                        "Son Heung-min"
                );
    }


    @Test
    @DisplayName("검색 결과가 50개를 초과하면 Slice 페이징 정보가 정상적으로 반환된다.")
    void searchPlayer_paging() {

        // given
        IntStream.rangeClosed(1, 51)
                .forEach(i ->
                        savePlayer(
                                String.format("Player%02d", i),
                                (long) i
                        )
                );

        // when
        PlayersResponseDto firstPage =
                playerService.getPlayers(0, "player");

        PlayersResponseDto secondPage =
                playerService.getPlayers(1, "player");

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
    // 예외 상황
    // ==================================================

    @Test
    @DisplayName("페이지 번호가 음수이면 예외가 발생한다.")
    void searchPlayer_negativePage_fail() {

        // given
        int page = -1;

        // when & then
        assertThatThrownBy(() ->
                playerService.getPlayers(page, "Saka")
        )
                .isInstanceOf(InvalidPlayerSearchPageValueException.class)
                .hasMessage("페이지 번호는 0 이상이어야 합니다.");
    }


    // ==================================================
    // 경계 상황
    // ==================================================

    @Test
    @DisplayName("검색어 앞뒤 공백을 제거하고 검색한다.")
    void searchPlayer_trimKeyword() {

        // given
        savePlayer("Bukayo Saka", 1L);
        savePlayer("Son Heung-min", 2L);

        // when
        PlayersResponseDto response =
                playerService.getPlayers(0, "   Saka   ");

        // then
        assertThat(response.getPlayers())
                .extracting(PlayerItemResponseDto::getPlayerName)
                .containsExactly("Bukayo Saka");
    }


    @Test
    @DisplayName("검색 조건에 맞는 선수가 없으면 빈 리스트를 반환한다.")
    void searchPlayer_notFound() {

        // given
        savePlayer("Bukayo Saka", 1L);
        savePlayer("Son Heung-min", 2L);

        // when
        PlayersResponseDto response =
                playerService.getPlayers(0, "Messi");

        // then
        assertThat(response.getPlayers())
                .isEmpty();

        assertThat(response.isHasNext())
                .isFalse();

        assertThat(response.isHasPrevious())
                .isFalse();
    }


    @Test
    @DisplayName("검색어가 공백이면 모든 선수를 조회한다.")
    void searchPlayer_blankKeyword_findAll() {

        // given
        savePlayer("Son Heung-min", 1L);
        savePlayer("Bukayo Saka", 2L);
        savePlayer("Bruno Fernandes", 3L);

        // when
        PlayersResponseDto response =
                playerService.getPlayers(0, "   ");

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


    @Test
    @DisplayName("검색어가 null이면 모든 선수를 조회한다.")
    void searchPlayer_nullKeyword_findAll() {

        // given
        savePlayer("Son Heung-min", 1L);
        savePlayer("Bukayo Saka", 2L);

        // when
        PlayersResponseDto response =
                playerService.getPlayers(0, null);

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


    @Test
    @DisplayName("존재하지 않는 페이지를 조회하면 빈 리스트를 반환한다.")
    void searchPlayer_pageOutOfRange() {

        // given
        savePlayer("Bukayo Saka", 1L);
        savePlayer("Son Heung-min", 2L);

        // when
        PlayersResponseDto response =
                playerService.getPlayers(100, "");

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
}