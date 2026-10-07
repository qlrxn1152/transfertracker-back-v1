package com.dhoon.transfertracker.internal.player.service.impl;

import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.dto.response.PlayerItemResponseDto;
import com.dhoon.transfertracker.internal.player.exception.NotFoundPlayerException;
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
        PlayerOrchestrationService.class,
        PlayerTxService.class
})
@ActiveProfiles("test")

/*
 * 실제 요청 구조:
 *
 * Client
 *      ↓
 * Controller
 *      ↓
 * PlayerOrchestrationService
 *      → Transaction X
 *      ↓
 * PlayerTxService
 *      → readOnly Transaction
 *      → Repository
 *      → DTO 생성
 *      ↓
 * Transaction 종료
 *      ↓
 * PlayerOrchestrationService
 *
 *
 * 실제 Transaction 경계를 검증하기 위해
 * DataJpaTest의 기본 Test Transaction을 사용하지 않는다.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PlayerDetailTest {

    @Autowired
    PlayerOrchestrationService playerOrchestrationService;

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
    // 정상 상황
    // ==================================================

    /*
     * [신규 - 중요]
     *
     * 현재 소속팀이 존재한다면
     * TeamPlayer를 이용해서
     *
     * Player
     * +
     * Team
     *
     * 정보를 하나의 DTO로 만들어 반환한다.
     */
    @Test
    @DisplayName("현재 소속팀이 있는 선수는 팀 정보까지 함께 반환한다.")
    void getPlayer_withTeam() {

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
                playerRepository.save(
                        Player.of(
                                "Bukayo Saka",
                                1L
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
                playerOrchestrationService.getPlayer(
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
                        "Arsenal"
                );

        assertThat(response.getTeamNameKo())
                .isEqualTo(
                        "아스널"
                );

        assertThat(response.getPhotoUrl())
                .isEqualTo(
                        "https://media.api-sports.io/football/players/1.png"
                );
    }


    /*
     * [신규 - 정책 테스트]
     *
     * Player는 존재하지만 TeamPlayer가 없다.
     *
     * 즉 현재 소속팀이 없는 선수.
     *
     * 단건 조회 자체는 가능하고
     * Team 정보만 null로 반환한다.
     */
    @Test
    @DisplayName("현재 소속팀이 없는 선수는 선수 정보만 반환한다.")
    void getPlayer_withoutTeam() {

        // given
        Player player =
                playerRepository.save(
                        Player.of(
                                "Free Agent Player",
                                2L
                        )
                );


        // when
        PlayerItemResponseDto response =
                playerOrchestrationService.getPlayer(
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

        assertThat(response.getTeamNameKo())
                .isNull();

        assertThat(response.getPhotoUrl())
                .isEqualTo(
                        "https://media.api-sports.io/football/players/2.png"
                );
    }


    /*
     * [신규]
     *
     * PlayerNameKo가 있다면
     * PlayerItemResponseDto는 getDisplayName()을 사용하므로
     * 한글 이름을 반환해야 한다.
     */
    @Test
    @DisplayName("선수 한글 이름이 존재하면 한글 이름을 반환한다.")
    void getPlayer_withKoreanName() {

        // given
        Player player =
                Player.of(
                        "Son Heung-min",
                        3L
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
                playerOrchestrationService.getPlayer(
                        player.getId()
                );


        // then
        assertThat(response.getPlayerName())
                .isEqualTo(
                        "손흥민"
                );
    }


    // ==================================================
    // 실패 상황
    // ==================================================

    /*
     * [신규 - 실패]
     *
     * TeamPlayer 조회 실패
     *      ↓
     * Player 조회
     *      ↓
     * Player도 없음
     *      ↓
     * NotFoundPlayerException
     */
    @Test
    @DisplayName("존재하지 않는 선수를 조회하면 예외가 발생한다.")
    void getPlayer_notFound() {

        // given
        Long playerId = 999999L;


        // when & then
        assertThatThrownBy(() ->
                playerOrchestrationService.getPlayer(
                        playerId
                )
        )
                .isInstanceOf(
                        NotFoundPlayerException.class
                );
    }
}