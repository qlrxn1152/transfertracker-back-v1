package com.dhoon.transfertracker.internal.team.service.impl;

import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.dto.response.TeamPageInfosResponseDto;
import com.dhoon.transfertracker.internal.team.exception.InvalidTeamIdException;
import com.dhoon.transfertracker.internal.team.exception.NotFoundTeamException;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.teamplayer.domain.TeamPlayer;
import com.dhoon.transfertracker.internal.teamplayer.dto.response.TeamPlayerItemResponseDto;
import com.dhoon.transfertracker.internal.teamplayer.repository.TeamPlayerRepository;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPost;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.transferpost.dto.response.TransferPostItemResponseDto;
import com.dhoon.transfertracker.internal.transferpost.repository.TransferPostRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@DataJpaTest
@Import({
        TeamOrchestrationService.class,
        TeamTxService.class
})
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TeamPageInfoTest {

    @Autowired
    TeamOrchestrationService teamOrchestrationService;

    @Autowired
    TeamRepository teamRepository;

    @Autowired
    PlayerRepository playerRepository;

    @Autowired
    TeamPlayerRepository teamPlayerRepository;

    @Autowired
    TransferPostRepository transferPostRepository;


    /*
     * Test Transaction이 없기 때문에
     * FK 자식 Entity부터 직접 제거한다.
     */
    @AfterEach
    void clearDatabase() {

        transferPostRepository.deleteAll();
        teamPlayerRepository.deleteAll();

        playerRepository.deleteAll();
        teamRepository.deleteAll();
    }


    // ==================================================
    // 정상 상황
    // ==================================================

    @Test
    @DisplayName("팀 페이지에 필요한 팀 기본 정보를 조회할 수 있다.")
    void getTeamInfo_teamInfo() {

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


        // when
        TeamPageInfosResponseDto response =
                teamOrchestrationService.getTeamInfo(
                        arsenal.getId()
                );


        // then
        assertThat(response.getTeams().getTeamId())
                .isEqualTo(
                        arsenal.getId()
                );

        assertThat(response.getTeams().getTeamName())
                .isEqualTo(
                        "아스널"
                );

        assertThat(response.getTeams().getTeamPlayerCount())
                .isZero();

        assertThat(response.getTeams().getLogoUrl())
                .isEqualTo(
                        "https://media.api-sports.io/football/teams/42.png"
                );
    }


    /*
     * countByTeamId() 결과가
     * 실제 현재 소속 선수 수와 일치하는지 확인한다.
     */
    @Test
    @DisplayName("팀 페이지에는 현재 소속 선수 수가 포함된다.")
    void getTeamInfo_playerCount() {

        // given
        Team arsenal =
                saveTeam(
                        "Arsenal",
                        42L
                );

        savePlayerWithTeam(
                "Bukayo Saka",
                1L,
                arsenal
        );

        savePlayerWithTeam(
                "Martin Odegaard",
                2L,
                arsenal
        );


        // when
        TeamPageInfosResponseDto response =
                teamOrchestrationService.getTeamInfo(
                        arsenal.getId()
                );


        // then
        assertThat(response.getTeams().getTeamPlayerCount())
                .isEqualTo(2L);
    }


    /*
     * 중요.
     *
     * TeamPlayer.player는 LAZY다.
     *
     * TeamTxService Transaction 안에서
     *
     * TeamPlayer
     *      ↓
     * Player
     *      ↓
     * TeamPlayerItemResponseDto
     *
     * 변환이 끝나야 한다.
     *
     * Test 자체 Transaction이 없으므로
     * Entity를 밖으로 반환하는 구조라면
     * Transaction boundary 문제가 드러날 수 있다.
     */
    @Test
    @DisplayName("팀 페이지의 선수 정보는 Transaction 안에서 DTO로 변환되어 반환된다.")
    void getTeamInfo_players() {

        // given
        Team arsenal =
                saveTeam(
                        "Arsenal",
                        42L
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
        TeamPageInfosResponseDto response =
                teamOrchestrationService.getTeamInfo(
                        arsenal.getId()
                );


        // then
        assertThat(response.getPlayers().getTeamPlayers())
                .hasSize(2);

        assertThat(response.getPlayers().getTeamPlayers())
                .extracting(
                        TeamPlayerItemResponseDto::getPlayerId
                )
                .containsExactlyInAnyOrder(
                        saka.getId(),
                        odegaard.getId()
                );

        assertThat(response.getPlayers().getTeamPlayers())
                .extracting(
                        TeamPlayerItemResponseDto::getPlayerName
                )
                .containsExactlyInAnyOrder(
                        "Bukayo Saka",
                        "Martin Odegaard"
                );
    }


    @Test
    @DisplayName("팀 페이지 선수 정보에서도 한글 선수명이 존재하면 한글 이름을 반환한다.")
    void getTeamInfo_playerKoreanName() {

        // given
        Team arsenal =
                saveTeam(
                        "Arsenal",
                        42L
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
        TeamPageInfosResponseDto response =
                teamOrchestrationService.getTeamInfo(
                        arsenal.getId()
                );


        // then
        assertThat(response.getPlayers().getTeamPlayers())
                .hasSize(1);

        assertThat(
                response.getPlayers()
                        .getTeamPlayers()
                        .get(0)
                        .getPlayerName()
        )
                .isEqualTo(
                        "부카요 사카"
                );
    }


    @Test
    @DisplayName("팀에 연결된 게시물 정보를 함께 반환한다.")
    void getTeamInfo_posts() {

        // given
        Team arsenal =
                saveTeam(
                        "Arsenal",
                        42L
                );

        TransferPost firstPost =
                saveTransferPost(
                        arsenal,
                        "Arsenal transfer news",
                        "post-1"
                );

        TransferPost secondPost =
                saveTransferPost(
                        arsenal,
                        "Another Arsenal news",
                        "post-2"
                );


        // when
        TeamPageInfosResponseDto response =
                teamOrchestrationService.getTeamInfo(
                        arsenal.getId()
                );


        // then
        assertThat(response.getPosts().getPosts())
                .hasSize(2);

        assertThat(response.getPosts().getPosts())
                .extracting(
                        TransferPostItemResponseDto::getPostId
                )
                .containsExactlyInAnyOrder(
                        firstPost.getId(),
                        secondPost.getId()
                );

        assertThat(response.getPosts().getPosts())
                .extracting(
                        TransferPostItemResponseDto::getContent
                )
                .containsExactlyInAnyOrder(
                        "Arsenal transfer news",
                        "Another Arsenal news"
                );
    }


    /*
     * 다른 팀의 데이터가 섞이지 않는지 확인.
     *
     * TeamPlayer / TransferPost 모두
     * teamId 조건이 정확히 적용되어야 한다.
     */
    @Test
    @DisplayName("팀 페이지에는 해당 팀의 선수와 게시물만 반환된다.")
    void getTeamInfo_excludeOtherTeamData() {

        // given
        Team arsenal =
                saveTeam(
                        "Arsenal",
                        42L
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


        saveTransferPost(
                arsenal,
                "Arsenal Post",
                "post-1"
        );

        saveTransferPost(
                barcelona,
                "Barcelona Post",
                "post-2"
        );


        // when
        TeamPageInfosResponseDto response =
                teamOrchestrationService.getTeamInfo(
                        arsenal.getId()
                );


        // then
        assertThat(response.getTeams().getTeamPlayerCount())
                .isEqualTo(1L);

        assertThat(response.getPlayers().getTeamPlayers())
                .extracting(
                        TeamPlayerItemResponseDto::getPlayerName
                )
                .containsExactly(
                        "Bukayo Saka"
                );

        assertThat(response.getPosts().getPosts())
                .extracting(
                        TransferPostItemResponseDto::getContent
                )
                .containsExactly(
                        "Arsenal Post"
                );
    }


    // ==================================================
    // 경계 상황
    // ==================================================

    @Test
    @DisplayName("선수와 게시물이 없는 팀은 빈 목록을 반환한다.")
    void getTeamInfo_emptyPlayersAndPosts() {

        // given
        Team arsenal =
                saveTeam(
                        "Arsenal",
                        42L
                );


        // when
        TeamPageInfosResponseDto response =
                teamOrchestrationService.getTeamInfo(
                        arsenal.getId()
                );


        // then
        assertThat(response.getTeams().getTeamPlayerCount())
                .isZero();

        assertThat(response.getPlayers().getTeamPlayers())
                .isEmpty();

        assertThat(response.getPosts().getPosts())
                .isEmpty();
    }


    // ==================================================
    // 실패 상황
    // ==================================================

    @Test
    @DisplayName("존재하지 않는 팀 페이지를 조회하면 예외가 발생한다.")
    void getTeamInfo_notFound() {

        // given
        Long teamId = 999999L;


        // when & then
        assertThatThrownBy(() ->
                teamOrchestrationService.getTeamInfo(
                        teamId
                )
        )
                .isInstanceOf(
                        NotFoundTeamException.class
                );
    }


    @Test
    @DisplayName("teamId가 null이면 팀 페이지 조회 전에 예외가 발생한다.")
    void getTeamInfo_nullTeamId() {

        // when & then
        assertThatThrownBy(() ->
                teamOrchestrationService.getTeamInfo(
                        null
                )
        )
                .isInstanceOf(
                        InvalidTeamIdException.class
                );
    }


    // ==================================================
    // Fixture
    // ==================================================

    private Team saveTeam(
            String teamName,
            Long apiFootballId
    ) {

        return saveTeam(
                teamName,
                apiFootballId,
                LeagueCode.EPL
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


    private TransferPost saveTransferPost(
            Team team,
            String content,
            String externalPostId
    ) {

        TransferPost post =
                TransferPost.of(
                        TransferPostSource.FABRIZIO_ROMANO,
                        content,
                        externalPostId,
                        Instant.now(),
                        true
                );

        post.assignTeam(
                team
        );

        return transferPostRepository.save(
                post
        );
    }
}