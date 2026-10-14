package com.dhoon.transfertracker.internal.player.query.service.impl;

import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.exception.InvalidPlayerIdException;
import com.dhoon.transfertracker.internal.player.exception.NotFoundPlayerException;
import com.dhoon.transfertracker.internal.player.query.dto.response.PlayerItemResponseDto;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@DataJpaTest
@Import({
        PlayerQueryOrchestrationService.class,
        PlayerQueryTxService.class
})
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PlayerDetailQueryTest {


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
    // 정상
    // ==================================================

    @Test
    @DisplayName("현재 소속팀이 있는 선수는 팀 정보까지 함께 반환한다.")
    void getPlayer_withTeam() {

        // given
        Team arsenal =
                Team.of(
                        "Arsenal",
                        42L,
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
                playerRepository.save(
                        Player.of(
                                "Bukayo Saka",
                                10L
                        )
                );


        teamPlayerRepository.save(
                TeamPlayer.of(
                        arsenal,
                        saka
                )
        );


        // when
        PlayerItemResponseDto response =
                playerQueryOrchestrationService
                        .getPlayer(
                                saka.getId()
                        );


        // then
        assertThat(response.getPlayerId())
                .isEqualTo(
                        saka.getId()
                );

        assertThat(response.getPlayerName())
                .isEqualTo(
                        "Bukayo Saka"
                );

        assertThat(response.getTeamName())
                .isEqualTo(
                        "아스널"
                );

        assertThat(response.getPhotoUrl())
                .isEqualTo(
                        "https://media.api-sports.io/football/players/10.png"
                );
    }


    @Test
    @DisplayName("현재 소속팀이 없는 선수도 조회할 수 있고 teamName은 null이다.")
    void getPlayer_withoutTeam() {

        // given
        Player player =
                playerRepository.save(
                        Player.of(
                                "Free Agent Player",
                                20L
                        )
                );


        // when
        PlayerItemResponseDto response =
                playerQueryOrchestrationService
                        .getPlayer(
                                player.getId()
                        );


        // then
        assertThat(response.getPlayerId())
                .isEqualTo(
                        player.getId()
                );

        assertThat(response.getPlayerName())
                .isEqualTo(
                        "Free Agent Player"
                );

        assertThat(response.getTeamName())
                .isNull();

        assertThat(response.getPhotoUrl())
                .isEqualTo(
                        "https://media.api-sports.io/football/players/20.png"
                );
    }


    @Test
    @DisplayName("선수 한글 이름이 존재하면 displayName으로 한글 이름을 반환한다.")
    void getPlayer_withKoreanName() {

        // given
        Player player =
                Player.of(
                        "Son Heung-min",
                        30L
                );

        player.assignKoPlayerName(
                "손흥민"
        );

        player =
                playerRepository.save(
                        player
                );


        // when
        PlayerItemResponseDto response =
                playerQueryOrchestrationService
                        .getPlayer(
                                player.getId()
                        );


        // then
        assertThat(response.getPlayerName())
                .isEqualTo(
                        "손흥민"
                );

        assertThat(response.getTeamName())
                .isNull();
    }


    @Test
    @DisplayName("선수와 팀 모두 한글 이름이 존재하면 각각 displayName을 반환한다.")
    void getPlayer_withKoreanPlayerAndTeamName() {

        // given
        Team arsenal =
                Team.of(
                        "Arsenal",
                        42L,
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
                Player.of(
                        "Bukayo Saka",
                        10L
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
        PlayerItemResponseDto response =
                playerQueryOrchestrationService
                        .getPlayer(
                                saka.getId()
                        );


        // then
        assertThat(response.getPlayerName())
                .isEqualTo(
                        "부카요 사카"
                );

        assertThat(response.getTeamName())
                .isEqualTo(
                        "아스널"
                );
    }


    // ==================================================
    // 실패
    // ==================================================

    @Test
    @DisplayName("playerId가 null이면 예외가 발생한다.")
    void getPlayer_nullPlayerId() {

        // when & then
        assertThatThrownBy(() ->
                playerQueryOrchestrationService
                        .getPlayer(
                                null
                        )
        )
                .isInstanceOf(
                        InvalidPlayerIdException.class
                );
    }


    @Test
    @DisplayName("존재하지 않는 선수를 조회하면 예외가 발생한다.")
    void getPlayer_notFound() {

        // given
        Long playerId = 999999L;


        // when & then
        assertThatThrownBy(() ->
                playerQueryOrchestrationService
                        .getPlayer(
                                playerId
                        )
        )
                .isInstanceOf(
                        NotFoundPlayerException.class
                );
    }
}