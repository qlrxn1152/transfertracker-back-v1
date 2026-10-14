package com.dhoon.transfertracker.internal.team.query.service.impl;

import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.exception.InvalidTeamIdException;
import com.dhoon.transfertracker.internal.team.exception.NotFoundTeamException;
import com.dhoon.transfertracker.internal.team.query.dto.response.TeamPageInfosResponseDto;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.teamplayer.domain.TeamPlayer;
import com.dhoon.transfertracker.internal.teamplayer.query.dto.response.TeamPlayerItemResponseDto;
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
        TeamQueryOrchestrationService.class,
        TeamQueryTxService.class
})
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TeamPageInfoQueryTest {


    @Autowired
    TeamQueryOrchestrationService teamQueryOrchestrationService;

    @Autowired
    TeamRepository teamRepository;

    @Autowired
    PlayerRepository playerRepository;

    @Autowired
    TeamPlayerRepository teamPlayerRepository;

    @Autowired
    TransferPostRepository transferPostRepository;


    @AfterEach
    void clearDatabase() {

        transferPostRepository.deleteAll();
        teamPlayerRepository.deleteAll();

        playerRepository.deleteAll();
        teamRepository.deleteAll();
    }


    // ==================================================
    // Team 정보
    // ==================================================

    @Test
    @DisplayName("팀 페이지에 필요한 팀 기본 정보를 조회할 수 있다.")
    void getTeamInfo_teamInfo() {

        // given
        Team arsenal = Team.of(
                "Arsenal",
                42L,
                LeagueCode.EPL
        );

        arsenal.assignKoTeamName(
                "아스널"
        );

        arsenal = teamRepository.save(
                arsenal
        );


        // when
        TeamPageInfosResponseDto response =
                teamQueryOrchestrationService.getTeamInfo(
                        arsenal.getId()
                );


        // then
        assertThat(response.getTeams())
                .isNotNull();

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


    // ==================================================
    // Player Count
    // ==================================================

    @Test
    @DisplayName("팀 페이지에는 현재 소속 선수 수가 포함된다.")
    void getTeamInfo_playerCount() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                42L
        );


        savePlayerWithTeam(
                "Bukayo Saka",
                10L,
                arsenal
        );

        savePlayerWithTeam(
                "Martin Odegaard",
                20L,
                arsenal
        );


        // when
        TeamPageInfosResponseDto response =
                teamQueryOrchestrationService.getTeamInfo(
                        arsenal.getId()
                );


        // then
        assertThat(
                response.getTeams()
                        .getTeamPlayerCount()
        )
                .isEqualTo(2L);
    }


    // ==================================================
    // Player 목록
    // ==================================================

    @Test
    @DisplayName("팀 페이지에는 현재 소속 선수 목록이 포함된다.")
    void getTeamInfo_players() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                42L
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
        TeamPageInfosResponseDto response =
                teamQueryOrchestrationService.getTeamInfo(
                        arsenal.getId()
                );


        // then
        assertThat(response.getPlayers())
                .isNotNull();

        assertThat(
                response.getPlayers()
                        .getTeamPlayers()
        )
                .hasSize(2);


        assertThat(
                response.getPlayers()
                        .getTeamPlayers()
        )
                .extracting(
                        TeamPlayerItemResponseDto::getPlayerId
                )
                .containsExactlyInAnyOrder(
                        saka.getId(),
                        odegaard.getId()
                );


        assertThat(
                response.getPlayers()
                        .getTeamPlayers()
        )
                .extracting(
                        TeamPlayerItemResponseDto::getPlayerName
                )
                .containsExactlyInAnyOrder(
                        "Bukayo Saka",
                        "Martin Odegaard"
                );
    }


    @Test
    @DisplayName("팀 페이지의 선수 DTO에는 선수 사진 URL이 포함된다.")
    void getTeamInfo_playerDtoMapping() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                42L
        );


        Player saka = savePlayerWithTeam(
                "Bukayo Saka",
                10L,
                arsenal
        );


        // when
        TeamPageInfosResponseDto response =
                teamQueryOrchestrationService.getTeamInfo(
                        arsenal.getId()
                );


        // then
        TeamPlayerItemResponseDto player =
                response.getPlayers()
                        .getTeamPlayers()
                        .get(0);


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
    @DisplayName("팀 페이지 선수 정보에서도 한글 선수명이 존재하면 displayName을 반환한다.")
    void getTeamInfo_playerKoreanName() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                42L
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
        TeamPageInfosResponseDto response =
                teamQueryOrchestrationService.getTeamInfo(
                        arsenal.getId()
                );


        // then
        assertThat(
                response.getPlayers()
                        .getTeamPlayers()
        )
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


    // ==================================================
    // TransferPost
    // ==================================================

    @Test
    @DisplayName("팀 페이지에는 해당 팀에 연결된 게시물 목록이 포함된다.")
    void getTeamInfo_posts() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                42L
        );


        TransferPost firstPost = saveTransferPost(
                arsenal,
                "Arsenal transfer news",
                "post-1"
        );

        TransferPost secondPost = saveTransferPost(
                arsenal,
                "Another Arsenal news",
                "post-2"
        );


        // when
        TeamPageInfosResponseDto response =
                teamQueryOrchestrationService.getTeamInfo(
                        arsenal.getId()
                );


        // then
        assertThat(response.getPosts())
                .isNotNull();

        assertThat(
                response.getPosts()
                        .getPosts()
        )
                .hasSize(2);


        assertThat(
                response.getPosts()
                        .getPosts()
        )
                .extracting(
                        TransferPostItemResponseDto::getPostId
                )
                .containsExactlyInAnyOrder(
                        firstPost.getId(),
                        secondPost.getId()
                );


        assertThat(
                response.getPosts()
                        .getPosts()
        )
                .extracting(
                        TransferPostItemResponseDto::getContent
                )
                .containsExactlyInAnyOrder(
                        "Arsenal transfer news",
                        "Another Arsenal news"
                );
    }


    @Test
    @DisplayName("팀 페이지의 게시물 정보는 DTO로 정상 변환된다.")
    void getTeamInfo_postDtoMapping() {

        // given
        Team arsenal = Team.of(
                "Arsenal",
                42L,
                LeagueCode.EPL
        );

        arsenal.assignKoTeamName(
                "아스널"
        );

        arsenal = teamRepository.save(
                arsenal
        );


        Instant createdAt =
                Instant.parse(
                        "2026-07-01T12:00:00Z"
                );


        TransferPost post = TransferPost.of(
                TransferPostSource.FABRIZIO_ROMANO,
                "Arsenal transfer news",
                "external-post-1",
                createdAt,
                true
        );

        post.assignTeam(
                arsenal
        );

        post = transferPostRepository.save(
                post
        );


        // when
        TeamPageInfosResponseDto response =
                teamQueryOrchestrationService.getTeamInfo(
                        arsenal.getId()
                );


        // then
        TransferPostItemResponseDto postResponse =
                response.getPosts()
                        .getPosts()
                        .get(0);


        assertThat(postResponse.getPostId())
                .isEqualTo(
                        post.getId()
                );

        assertThat(postResponse.getExternalPostId())
                .isEqualTo(
                        "external-post-1"
                );

        assertThat(postResponse.getContent())
                .isEqualTo(
                        "Arsenal transfer news"
                );

        assertThat(postResponse.getSource())
                .isEqualTo(
                        TransferPostSource.FABRIZIO_ROMANO
                );

        assertThat(postResponse.getPostCreatedAt())
                .isEqualTo(
                        createdAt
                );

        assertThat(postResponse.isRelateTransfer())
                .isTrue();

        assertThat(postResponse.getTeamId())
                .isEqualTo(
                        arsenal.getId()
                );

        assertThat(postResponse.getTeamName())
                .isEqualTo(
                        "아스널"
                );
    }


    // ==================================================
    // 다른 Team 데이터 제외
    // ==================================================

    @Test
    @DisplayName("팀 페이지에는 해당 팀의 선수와 게시물만 반환한다.")
    void getTeamInfo_excludeOtherTeamData() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                42L
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
                teamQueryOrchestrationService.getTeamInfo(
                        arsenal.getId()
                );


        // then
        assertThat(
                response.getTeams()
                        .getTeamPlayerCount()
        )
                .isEqualTo(1L);


        assertThat(
                response.getPlayers()
                        .getTeamPlayers()
        )
                .extracting(
                        TeamPlayerItemResponseDto::getPlayerName
                )
                .containsExactly(
                        "Bukayo Saka"
                );


        assertThat(
                response.getPosts()
                        .getPosts()
        )
                .extracting(
                        TransferPostItemResponseDto::getContent
                )
                .containsExactly(
                        "Arsenal Post"
                );
    }


    // ==================================================
    // 경계
    // ==================================================

    @Test
    @DisplayName("선수와 게시물이 없는 팀은 빈 목록과 선수 수 0을 반환한다.")
    void getTeamInfo_emptyPlayersAndPosts() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                42L
        );


        // when
        TeamPageInfosResponseDto response =
                teamQueryOrchestrationService.getTeamInfo(
                        arsenal.getId()
                );


        // then
        assertThat(
                response.getTeams()
                        .getTeamPlayerCount()
        )
                .isZero();


        assertThat(
                response.getPlayers()
                        .getTeamPlayers()
        )
                .isEmpty();


        assertThat(
                response.getPosts()
                        .getPosts()
        )
                .isEmpty();
    }


    // ==================================================
    // 실패
    // ==================================================

    @Test
    @DisplayName("teamId가 null이면 팀 페이지 조회 전에 예외가 발생한다.")
    void getTeamInfo_nullTeamId() {

        // when & then
        assertThatThrownBy(() ->
                teamQueryOrchestrationService.getTeamInfo(
                        null
                )
        )
                .isInstanceOf(
                        InvalidTeamIdException.class
                );
    }


    @Test
    @DisplayName("존재하지 않는 팀 페이지를 조회하면 예외가 발생한다.")
    void getTeamInfo_notFound() {

        // given
        Long teamId = 999999L;


        // when & then
        assertThatThrownBy(() ->
                teamQueryOrchestrationService.getTeamInfo(
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