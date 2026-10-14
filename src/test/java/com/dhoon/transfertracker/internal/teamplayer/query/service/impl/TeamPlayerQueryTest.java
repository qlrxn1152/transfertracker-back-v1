package com.dhoon.transfertracker.internal.teamplayer.query.service.impl;

import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.exception.InvalidTeamIdException;
import com.dhoon.transfertracker.internal.team.exception.NotFoundTeamException;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.teamplayer.domain.TeamPlayer;
import com.dhoon.transfertracker.internal.teamplayer.query.dto.response.TeamPlayerItemResponseDto;
import com.dhoon.transfertracker.internal.teamplayer.query.dto.response.TeamPlayersResponseDto;
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
        TeamPlayerQueryOrchestrationService.class,
        TeamPlayerQueryTxService.class
})
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TeamPlayerQueryTest {


    @Autowired
    TeamPlayerQueryOrchestrationService teamPlayerQueryOrchestrationService;

    @Autowired
    TeamPlayerRepository teamPlayerRepository;

    @Autowired
    PlayerRepository playerRepository;

    @Autowired
    TeamRepository teamRepository;


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
    @DisplayName("특정 팀에 현재 소속된 선수들을 조회할 수 있다.")
    void getTeamPlayers_success() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                42L,
                LeagueCode.EPL
        );


        Player saka = savePlayerWithTeam(
                "Bukayo Saka",
                10L,
                arsenal
        );

        Player odegaard = savePlayerWithTeam(
                "Martin Odegaard",
                20L,
                arsenal
        );


        // when
        TeamPlayersResponseDto response =
                teamPlayerQueryOrchestrationService.getTeamPlayers(
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


    @Test
    @DisplayName("다른 팀에 소속된 선수는 조회하지 않는다.")
    void getTeamPlayers_excludeOtherTeam() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                42L,
                LeagueCode.EPL
        );

        Team barcelona = saveTeam(
                "Barcelona",
                529L,
                LeagueCode.LA_LIGA
        );


        savePlayerWithTeam(
                "Bukayo Saka",
                10L,
                arsenal
        );

        savePlayerWithTeam(
                "Pedri",
                20L,
                barcelona
        );


        // when
        TeamPlayersResponseDto response =
                teamPlayerQueryOrchestrationService.getTeamPlayers(
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
    }


    // ==================================================
    // DTO Mapping
    // ==================================================

    @Test
    @DisplayName("소속 선수 정보는 Transaction 내부에서 DTO로 변환되어 반환된다.")
    void getTeamPlayers_dtoMapping() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                42L,
                LeagueCode.EPL
        );


        Player saka = savePlayerWithTeam(
                "Bukayo Saka",
                10L,
                arsenal
        );


        // when
        TeamPlayersResponseDto response =
                teamPlayerQueryOrchestrationService.getTeamPlayers(
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


    @Test
    @DisplayName("선수 한글 이름이 존재하면 displayName으로 한글 이름을 반환한다.")
    void getTeamPlayers_withKoreanPlayerName() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                42L,
                LeagueCode.EPL
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


        teamPlayerRepository.save(
                TeamPlayer.of(
                        arsenal,
                        saka
                )
        );


        // when
        TeamPlayersResponseDto response =
                teamPlayerQueryOrchestrationService.getTeamPlayers(
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
    // 경계
    // ==================================================

    @Test
    @DisplayName("팀은 존재하지만 현재 소속 선수가 없으면 빈 리스트를 반환한다.")
    void getTeamPlayers_empty() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                42L,
                LeagueCode.EPL
        );


        // when
        TeamPlayersResponseDto response =
                teamPlayerQueryOrchestrationService.getTeamPlayers(
                        arsenal.getId()
                );


        // then
        assertThat(response.getTeamPlayers())
                .isEmpty();
    }


    // ==================================================
    // 실패
    // ==================================================

    @Test
    @DisplayName("teamId가 null이면 예외가 발생한다.")
    void getTeamPlayers_nullTeamId() {

        // when & then
        assertThatThrownBy(() ->
                teamPlayerQueryOrchestrationService.getTeamPlayers(
                        null
                )
        )
                .isInstanceOf(
                        InvalidTeamIdException.class
                );
    }


    @Test
    @DisplayName("존재하지 않는 팀의 선수 목록을 조회하면 예외가 발생한다.")
    void getTeamPlayers_teamNotFound() {

        // given
        Long teamId = 999999L;


        // when & then
        assertThatThrownBy(() ->
                teamPlayerQueryOrchestrationService.getTeamPlayers(
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