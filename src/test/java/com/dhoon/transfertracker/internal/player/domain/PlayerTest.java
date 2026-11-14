package com.dhoon.transfertracker.internal.player.domain;


import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

public class PlayerTest {

    @Test
    @DisplayName(value = "정상적인 선수 정보로 Player를 생성한다.")
    void createPlayer() throws Exception {
        // given
        String playerName = "Bruno Fernandes";
        Long apiFootballApiId = 111L;

        // when
        Player player = Player.of(playerName, apiFootballApiId);

        // then
        assertThat(player.getPlayerName()).isEqualTo("Bruno Fernandes");
        assertThat(player.getDisplayName()).isEqualTo("Bruno Fernandes");
        assertThat(player.getApiFootballId()).isEqualTo(111L);
        assertThat(player.getPlayerNameKo()).isNull();
    }

    @Test
    @DisplayName(value = "선수 이름의 앞뒤 공백을 제거한 이후, Player 를 생성한다.")
    void normalize_PlayerName() throws Exception {
        // given
        String playerName = "      Bruno Fernandes       ";
        Long apiFootballApiId = 111L;

        // when
        Player player = Player.of(playerName, apiFootballApiId);

        // then
        assertThat(player.getPlayerName()).isEqualTo("Bruno Fernandes");
        assertThat(player.getDisplayName()).isEqualTo("Bruno Fernandes");
        assertThat(player.getApiFootballId()).isEqualTo(111L);
        assertThat(player.getPlayerNameKo()).isNull();
    }

    @Test
    @DisplayName(value = "선수 이름이 공백을 입력한경우 예외가 발생한다.")
    void create_player_fail_blankName() throws Exception {
        // given
        String playerName = "   ";
        Long apiFootballApiId = 111L;

        // when && then
        assertThatThrownBy(() -> Player.of(playerName, apiFootballApiId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("선수의 이름은 비어 있을 수 없습니다.");
    }

    @Test
    @DisplayName(value = "선수 이름에 null을 입력한경우 예외가 발생한다.")
    void create_player_fail_nullName() throws Exception {
        // given
        String playerName = null;
        Long apiFootballApiId = 111L;

        // when && then
        assertThatThrownBy(() -> Player.of(playerName, apiFootballApiId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("선수의 이름은 비어 있을 수 없습니다.");
    }

    @Test
    @DisplayName(value = "선수 외부 API ID 에 null 이 들어오거나, 자연수가 아닌경우 예외가 발생한다.")
    void create_player_fail_apiFootballApiId() throws Exception {
        // given
        String playerName = "Bruno Fernandes";
        Long apiFootballApiIdNull = null;

        // when && then
        assertThatThrownBy(() -> Player.of(playerName, apiFootballApiIdNull))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("선수의 API ID 의 값은 자연수이어야합니다.");

        assertThatThrownBy(() -> Player.of(playerName, Long.valueOf(-4)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("선수의 API ID 의 값은 자연수이어야합니다.");

        assertThatThrownBy(() -> Player.of(playerName, Long.valueOf(0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("선수의 API ID 의 값은 자연수이어야합니다.");
    }



    @Test
    @DisplayName(value = "정상적인 한국어 이름으로 Player 에게 한국어 이름을 할당한다.")
    void assign_koreanPlayerName() throws Exception {
        // given
        String playerName = "Bruno Fernandes";
        Long apiFootballApiIdNull = 111L;

        Player player = Player.of(playerName, apiFootballApiIdNull);

        String playerNameKo = "브루노 페르난데스";

        // when
        player.assignKoPlayerName(playerNameKo);

        // then
        assertThat(player.getDisplayName()).isEqualTo("브루노 페르난데스");
        assertThat(player.getApiFootballId()).isEqualTo(111L);
        assertThat(player.getPlayerNameKo()).isEqualTo("브루노 페르난데스");
        assertThat(player.getPlayerName()).isEqualTo("Bruno Fernandes");
    }

    @Test
    @DisplayName(value = "한국어 이름의 앞뒤 공백을 제거한후, Player에게 한국어이름을 할당한다.")
    void assign_koreanPlayerName_normalize() throws Exception {
        // given
        String playerName = "Bruno Fernandes";
        Long apiFootballApiIdNull = 111L;

        Player player = Player.of(playerName, apiFootballApiIdNull);

        String playerNameKo = "   브루노 페르난데스  ";

        // when
        player.assignKoPlayerName(playerNameKo);

        // then
        assertThat(player.getDisplayName()).isEqualTo("브루노 페르난데스");
        assertThat(player.getApiFootballId()).isEqualTo(111L);
        assertThat(player.getPlayerNameKo()).isEqualTo("브루노 페르난데스");
        assertThat(player.getPlayerName()).isEqualTo("Bruno Fernandes");
    }

    @Test
    @DisplayName(value = "공백인 한국어 이름을 요청할경우, 예외가 발생한다.")
    void assign_koreanPlayerName_fail_blank() throws Exception {
        // given
        String playerName = "Bruno Fernandes";
        Long apiFootballApiIdNull = 111L;

        Player player = Player.of(playerName, apiFootballApiIdNull);

        String playerNameKo = "   ";

        // when && then
        assertThatThrownBy(() -> player.assignKoPlayerName(playerNameKo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("선수의 한국어 이름은 비어 있을 수 없습니다.");
    }

    @Test
    @DisplayName(value = "null 값인 한국어 이름을 요청할경우, 예외가 발생한다.")
    void assign_koreanPlayerName_fail_null() throws Exception {
        // given
        String playerName = "Bruno Fernandes";
        Long apiFootballApiIdNull = 111L;

        Player player = Player.of(playerName, apiFootballApiIdNull);

        String playerNameKo = null;

        // when && then
        assertThatThrownBy(() -> player.assignKoPlayerName(playerNameKo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("선수의 한국어 이름은 비어 있을 수 없습니다.");
    }

    @Test
    @DisplayName(value = "선수의 이름을 요청한상황에서 선수의 한국어이름이 존재하지않고, 영어이름만 존재하는경우 영어이름을 반환한다.")
    void player_displayerName_noKoName() throws Exception {
        // given
        String playerName = "Bruno Fernandes";
        Long apiFootballApiIdNull = 111L;

        Player player = Player.of(playerName, apiFootballApiIdNull);

        // when
        String playerDisplayName = player.getDisplayName();

        // then
        assertThat(playerDisplayName).isEqualTo("Bruno Fernandes");
    }

    @Test
    @DisplayName(value = "선수의 이름을 요청한상황에서 선수의 한국어이름이 존재하는경우, 한국어 이름을 반환한다.")
    void player_displayerName_hasKoName() throws Exception {
        // given
        String playerName = "Bruno Fernandes";
        Long apiFootballApiIdNull = 111L;

        Player player = Player.of(playerName, apiFootballApiIdNull);

        String playerNameKo = "브루노 페르난데스";
        player.assignKoPlayerName(playerNameKo);

        // when
        String playerDisplayName = player.getDisplayName();

        // then
        assertThat(playerDisplayName).isEqualTo("브루노 페르난데스");
    }

    @Test
    @DisplayName("잘못된 한국어 이름으로 변경을 시도하면 기존 이름을 유지한다")
    void assignKoreanNameFailKeepsExistingName() {
        // given
        Player player = Player.of("Bruno Fernandes", 111L);
        player.assignKoPlayerName("브루노 페르난데스");

        // when
        assertThatThrownBy(() -> player.assignKoPlayerName("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("선수의 한국어 이름은 비어 있을 수 없습니다.");


        // then
        assertThat(player.getPlayerNameKo())
                .isEqualTo("브루노 페르난데스");
    }







}
