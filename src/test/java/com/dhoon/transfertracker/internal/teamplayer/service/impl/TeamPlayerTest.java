package com.dhoon.transfertracker.internal.teamplayer.service.impl;

import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.exception.InvalidTeamIdException;
import com.dhoon.transfertracker.internal.team.exception.NotFoundTeamException;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.teamplayer.domain.TeamPlayer;
import com.dhoon.transfertracker.internal.teamplayer.dto.response.TeamPlayerItemResponseDto;
import com.dhoon.transfertracker.internal.teamplayer.dto.response.TeamPlayersResponseDto;
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
        TeamPlayerOrchestrationService.class,
        TeamPlayerTxService.class
})
@ActiveProfiles("test")

/*
 * 실제 요청 흐름:
 *
 * Client
 *      ↓
 * Controller
 *      ↓
 * TeamPlayerOrchestrationService
 *      → Transaction X
 *      → teamId 검증
 *      ↓
 * TeamPlayerTxService
 *      → @Transactional(readOnly = true)
 *      → Team 존재 여부 확인
 *      → TeamPlayer 조회
 *      → Lazy Player 접근
 *      → DTO 변환
 *      ↓
 * Transaction 종료
 *      ↓
 * TeamPlayerOrchestrationService
 *
 *
 * 실제 Transaction 경계를 확인하기 위해
 * DataJpaTest의 기본 Test Transaction은 사용하지 않는다.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TeamPlayerTest {

    @Autowired
    TeamPlayerOrchestrationService teamPlayerOrchestrationService;

    @Autowired
    TeamPlayerRepository teamPlayerRepository;

    @Autowired
    PlayerRepository playerRepository;

    @Autowired
    TeamRepository teamRepository;


    /*
     * Test Transaction을 사용하지 않으므로
     * FK 자식 Entity부터 직접 삭제한다.
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

    @Test
    @DisplayName("특정 팀에 현재 소속된 선수들을 조회할 수 있다.")
    void getTeamPlayers_success() {

        // given
        Team arsenal =
                saveTeam(
                        "Arsenal",
                        42L,
                        LeagueCode.EPL
                );

        Player saka =
                savePlayerWithTeam(
                        "Bukayo Saka",
                        1L,
                        arsenal
                );

        Player odegaard =
                savePlayerWithTeam(
                        "Martin Odegaard",
                        2L,
                        arsenal
                );


        // when
        TeamPlayersResponseDto response =
                teamPlayerOrchestrationService.getTeamPlayers(
                        arsenal.getId()
                );


        // then
        assertThat(response.getTeamPlayers())
                .hasSize(2);

        assertThat(response.getTeamPlayers())
                .extracting(
                        TeamPlayerItemResponseDto::getPlayerId
                )
                .containsExactlyInAnyOrder(
                        saka.getId(),
                        odegaard.getId()
                );

        assertThat(response.getTeamPlayers())
                .extracting(
                        TeamPlayerItemResponseDto::getPlayerName
                )
                .containsExactlyInAnyOrder(
                        "Bukayo Saka",
                        "Martin Odegaard"
                );
    }


    /*
     * teamId 조건이 정확히 적용되는지 확인한다.
     *
     * 다른 Team의 TeamPlayer는
     * 결과에 포함되면 안 된다.
     */
    @Test
    @DisplayName("다른 팀에 소속된 선수는 조회되지 않는다.")
    void getTeamPlayers_excludeOtherTeam() {

        // given
        Team arsenal =
                saveTeam(
                        "Arsenal",
                        42L,
                        LeagueCode.EPL
                );

        Team barcelona =
                saveTeam(
                        "Barcelona",
                        529L,
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
        TeamPlayersResponseDto response =
                teamPlayerOrchestrationService.getTeamPlayers(
                        arsenal.getId()
                );


        // then
        assertThat(response.getTeamPlayers())
                .hasSize(1);

        assertThat(response.getTeamPlayers())
                .extracting(
                        TeamPlayerItemResponseDto::getPlayerName
                )
                .containsExactly(
                        "Bukayo Saka"
                );

        assertThat(response.getTeamPlayers())
                .extracting(
                        TeamPlayerItemResponseDto::getPlayerName
                )
                .doesNotContain(
                        "Pedri"
                );
    }


    /*
     * TeamPlayerItemResponseDto.of()
     *
     * 내부에서는 TeamPlayer의 LAZY Player에 접근한다.
     *
     * Test Transaction이 존재하지 않으므로
     * Player → DTO 변환이 TxService의 Transaction 안에서
     * 완료되어야 정상적으로 동작한다.
     */
    @Test
    @DisplayName("선수 정보는 Transaction 내부에서 DTO로 변환되어 반환된다.")
    void getTeamPlayers_dtoMapping() {

        // given
        Team arsenal =
                saveTeam(
                        "Arsenal",
                        42L,
                        LeagueCode.EPL
                );

        Player saka =
                savePlayerWithTeam(
                        "Bukayo Saka",
                        10L,
                        arsenal
                );


        // when
        TeamPlayersResponseDto response =
                teamPlayerOrchestrationService.getTeamPlayers(
                        arsenal.getId()
                );


        // then
        assertThat(response.getTeamPlayers())
                .hasSize(1);

        TeamPlayerItemResponseDto player =
                response.getTeamPlayers().get(0);

        assertThat(player.getPlayerId())
                .isEqualTo(
                        saka.getId()
                );

        assertThat(player.getPlayerName())
                .isEqualTo(
                        "Bukayo Saka"
                );

        assertThat(player.getPhotoUrl())
                .isEqualTo(
                        "https://media.api-sports.io/football/players/10.png"
                );
    }


    /*
     * Player.getDisplayName() 정책 확인.
     */
    @Test
    @DisplayName("선수 한글 이름이 존재하면 한글 이름을 반환한다.")
    void getTeamPlayers_withKoreanPlayerName() {

        // given
        Team arsenal =
                saveTeam(
                        "Arsenal",
                        42L,
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
        TeamPlayersResponseDto response =
                teamPlayerOrchestrationService.getTeamPlayers(
                        arsenal.getId()
                );


        // then
        assertThat(response.getTeamPlayers())
                .hasSize(1);

        assertThat(
                response.getTeamPlayers()
                        .get(0)
                        .getPlayerName()
        )
                .isEqualTo(
                        "부카요 사카"
                );
    }


    // ==================================================
    // 경계 상황
    // ==================================================

    /*
     * Team 자체는 존재하지만 TeamPlayer가 없다.
     *
     * 이는 오류가 아니라
     * "현재 소속 선수가 없는 팀"이라는 정상 상태다.
     */
    @Test
    @DisplayName("팀은 존재하지만 소속 선수가 없으면 빈 리스트를 반환한다.")
    void getTeamPlayers_empty() {

        // given
        Team arsenal =
                saveTeam(
                        "Arsenal",
                        42L,
                        LeagueCode.EPL
                );


        // when
        TeamPlayersResponseDto response =
                teamPlayerOrchestrationService.getTeamPlayers(
                        arsenal.getId()
                );


        // then
        assertThat(response.getTeamPlayers())
                .isEmpty();
    }


    // ==================================================
    // 실패 상황
    // ==================================================

    /*
     * null은 DB까지 내려갈 필요가 없는 잘못된 입력이다.
     *
     * OrchestrationService에서 차단한다.
     */
    @Test
    @DisplayName("teamId가 null이면 예외가 발생한다.")
    void getTeamPlayers_nullTeamId() {

        // when & then
        assertThatThrownBy(() ->
                teamPlayerOrchestrationService.getTeamPlayers(
                        null
                )
        )
                .isInstanceOf(
                        InvalidTeamIdException.class
                );
    }


    /*
     * teamId 자체는 null이 아니기 때문에
     * Orchestration validation은 통과한다.
     *
     * 이후 TxService에서 실제 Team 존재 여부를 확인한다.
     */
    @Test
    @DisplayName("존재하지 않는 팀의 선수 목록을 조회하면 예외가 발생한다.")
    void getTeamPlayers_teamNotFound() {

        // given
        Long teamId = 999999L;


        // when & then
        assertThatThrownBy(() ->
                teamPlayerOrchestrationService.getTeamPlayers(
                        teamId
                )
        )
                .isInstanceOf(
                        NotFoundTeamException.class
                );
    }


    // ==================================================
    // Fixture
    // ==================================================

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
                playerRepository.save(
                        Player.of(
                                playerName,
                                apiFootballId
                        )
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