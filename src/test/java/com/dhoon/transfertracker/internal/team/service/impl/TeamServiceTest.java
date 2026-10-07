package com.dhoon.transfertracker.internal.team.service.impl;

import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.dto.response.TeamItemResponseDto;
import com.dhoon.transfertracker.internal.team.dto.response.TeamPageInfosResponseDto;
import com.dhoon.transfertracker.internal.team.dto.response.TeamsResponseDto;
import com.dhoon.transfertracker.internal.team.exception.InvalidLeagueCodeValueException;
import com.dhoon.transfertracker.internal.team.exception.InvalidTeamIdException;
import com.dhoon.transfertracker.internal.team.exception.NotFoundTeamException;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.teamplayer.domain.TeamPlayer;
import com.dhoon.transfertracker.internal.teamplayer.repository.TeamPlayerRepository;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPost;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
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

/*
 * 실제 요청 구조:
 *
 * Client
 *      ↓
 * Controller
 *      ↓
 * TeamOrchestrationService
 *      → Transaction X
 *      → Request 값 검증
 *
 *      ↓
 *
 * TeamTxService
 *      → @Transactional(readOnly = true)
 *      → Repository 조회
 *      → Entity 사용
 *      → DTO 생성
 *
 *      ↓
 *
 * Transaction 종료
 *
 *      ↓
 *
 * TeamOrchestrationService
 *      → DTO 반환
 *
 *
 * @DataJpaTest의 기본 Test Transaction을 비활성화하여
 * 실제 Service Transaction 경계와 최대한 동일하게 검증한다.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TeamServiceTest {

    /*
     * 실제 요청 흐름의 진입점.
     *
     * TeamTxService를 테스트에서 직접 호출하지 않는다.
     */
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
     * Test Transaction을 사용하지 않기 때문에
     * 테스트 종료 후 자동 Rollback되지 않는다.
     *
     * FK 자식부터 삭제한다.
     */
    @AfterEach
    void clearDatabase() {

        transferPostRepository.deleteAll();
        teamPlayerRepository.deleteAll();
        playerRepository.deleteAll();
        teamRepository.deleteAll();
    }


    // ==================================================
    // getTeam
    // ==================================================

    /*
     * [정상]
     *
     * TeamOrchestrationService
     *      ↓
     * teamId 검증
     *      ↓
     * TeamTxService
     *      ↓
     * Team 조회
     *      ↓
     * DTO 생성
     */
    @Test
    @DisplayName("존재하는 팀을 단건 조회할 수 있다.")
    void getTeam_success() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                100L,
                LeagueCode.EPL
        );


        // when
        TeamItemResponseDto response =
                teamOrchestrationService.getTeam(
                        arsenal.getId()
                );


        // then
        assertThat(response.getTeamId())
                .isEqualTo(
                        arsenal.getId()
                );

        assertThat(response.getTeamName())
                .isEqualTo(
                        "Arsenal"
                );

        assertThat(response.getLogoUrl())
                .isEqualTo(
                        "https://media.api-sports.io/football/teams/100.png"
                );
    }


    /*
     * [정상 - 한글화 정책]
     *
     * TeamItemResponseDto는
     * Team.getDisplayName()을 사용한다.
     *
     * teamNameKo가 있으면 한글 이름을 우선 반환한다.
     */
    @Test
    @DisplayName("팀 한글 이름이 존재하면 한글 이름을 반환한다.")
    void getTeam_withKoreanName() {

        // given
        Team arsenal = saveTeamWithKoreanName(
                "Arsenal",
                "아스널",
                100L,
                LeagueCode.EPL
        );


        // when
        TeamItemResponseDto response =
                teamOrchestrationService.getTeam(
                        arsenal.getId()
                );


        // then
        assertThat(response.getTeamName())
                .isEqualTo(
                        "아스널"
                );
    }


    /*
     * [실패 - Orchestration 검증]
     *
     * null teamId는 Repository까지 내려가지 않고
     * OrchestrationService에서 차단한다.
     */
    @Test
    @DisplayName("팀 ID가 null이면 예외가 발생한다.")
    void getTeam_nullTeamId_fail() {

        // given
        Long teamId = null;


        // when & then
        assertThatThrownBy(() ->
                teamOrchestrationService.getTeam(
                        teamId
                )
        )
                .isInstanceOf(
                        InvalidTeamIdException.class
                )
                .hasMessage(
                        "잘못된 TeamId 값입니다."
                );
    }


    /*
     * [실패 - TxService]
     *
     * ID 자체는 유효하지만 DB에 Team이 존재하지 않는 경우.
     */
    @Test
    @DisplayName("존재하지 않는 팀을 조회하면 예외가 발생한다.")
    void getTeam_notFound_fail() {

        // given
        Long teamId = 999999L;


        // when & then
        assertThatThrownBy(() ->
                teamOrchestrationService.getTeam(
                        teamId
                )
        )
                .isInstanceOf(
                        NotFoundTeamException.class
                )
                .hasMessage(
                        "존재하지 않는 팀 입니다."
                );
    }


    // ==================================================
    // getTeams
    // ==================================================

    /*
     * [정상]
     *
     * 모든 Team을 DTO로 변환하여 반환한다.
     */
    @Test
    @DisplayName("전체 팀을 조회할 수 있다.")
    void getTeams_success() {

        // given
        saveTeam(
                "Arsenal",
                100L,
                LeagueCode.EPL
        );

        saveTeam(
                "Barcelona",
                200L,
                LeagueCode.LA_LIGA
        );

        saveTeam(
                "Bayern Munich",
                300L,
                LeagueCode.BUNDESLIGA
        );


        // when
        TeamsResponseDto response =
                teamOrchestrationService.getTeams();


        // then
        assertThat(response.getTeams())
                .hasSize(3);

        assertThat(response.getTeams())
                .extracting(
                        TeamItemResponseDto::getTeamName
                )
                .containsExactlyInAnyOrder(
                        "Arsenal",
                        "Barcelona",
                        "Bayern Munich"
                );
    }


    /*
     * [정상 - 한글 이름]
     */
    @Test
    @DisplayName("전체 팀 조회에서도 한글 이름이 존재하면 한글 이름을 반환한다.")
    void getTeams_withKoreanName() {

        // given
        saveTeamWithKoreanName(
                "Arsenal",
                "아스널",
                100L,
                LeagueCode.EPL
        );

        saveTeam(
                "Barcelona",
                200L,
                LeagueCode.LA_LIGA
        );


        // when
        TeamsResponseDto response =
                teamOrchestrationService.getTeams();


        // then
        assertThat(response.getTeams())
                .extracting(
                        TeamItemResponseDto::getTeamName
                )
                .containsExactlyInAnyOrder(
                        "아스널",
                        "Barcelona"
                );
    }


    /*
     * [경계]
     */
    @Test
    @DisplayName("등록된 팀이 없으면 빈 리스트를 반환한다.")
    void getTeams_empty() {

        // when
        TeamsResponseDto response =
                teamOrchestrationService.getTeams();


        // then
        assertThat(response.getTeams())
                .isEmpty();
    }


    // ==================================================
    // getLeagueTeams
    // ==================================================

    /*
     * [정상]
     *
     * 선택한 leagueCode에 속한 팀만 조회한다.
     */
    @Test
    @DisplayName("선택한 리그에 속한 팀만 조회한다.")
    void getLeagueTeams_success() {

        // given
        saveTeam(
                "Arsenal",
                100L,
                LeagueCode.EPL
        );

        saveTeam(
                "Manchester United",
                200L,
                LeagueCode.EPL
        );

        saveTeam(
                "Barcelona",
                300L,
                LeagueCode.LA_LIGA
        );


        // when
        TeamsResponseDto response =
                teamOrchestrationService.getLeagueTeams(
                        LeagueCode.EPL
                );


        // then
        assertThat(response.getTeams())
                .hasSize(2);

        assertThat(response.getTeams())
                .extracting(
                        TeamItemResponseDto::getTeamName
                )
                .containsExactlyInAnyOrder(
                        "Arsenal",
                        "Manchester United"
                );

        assertThat(response.getTeams())
                .extracting(
                        TeamItemResponseDto::getTeamName
                )
                .doesNotContain(
                        "Barcelona"
                );
    }


    /*
     * [경계]
     *
     * 유효한 LeagueCode지만 해당 리그 팀이 없는 경우
     * 예외가 아니라 빈 결과를 반환한다.
     */
    @Test
    @DisplayName("선택한 리그에 속한 팀이 없으면 빈 리스트를 반환한다.")
    void getLeagueTeams_empty() {

        // given
        saveTeam(
                "Arsenal",
                100L,
                LeagueCode.EPL
        );


        // when
        TeamsResponseDto response =
                teamOrchestrationService.getLeagueTeams(
                        LeagueCode.LA_LIGA
                );


        // then
        assertThat(response.getTeams())
                .isEmpty();
    }


    /*
     * [실패 - Orchestration 검증]
     *
     * leagueCode == null
     *
     * → TxService까지 내려가지 않는다.
     */
    @Test
    @DisplayName("리그 코드가 null이면 예외가 발생한다.")
    void getLeagueTeams_nullLeagueCode_fail() {

        // given
        LeagueCode leagueCode = null;


        // when & then
        assertThatThrownBy(() ->
                teamOrchestrationService.getLeagueTeams(
                        leagueCode
                )
        )
                .isInstanceOf(
                        InvalidLeagueCodeValueException.class
                )
                .hasMessage(
                        "잘못된 LeagueCode 값입니다."
                );
    }


    // ==================================================
    // getTeamInfo
    // ==================================================

    /*
     * [신규 - 중요]
     *
     * Team 상세 페이지 정보는 하나의 readOnly Transaction에서:
     *
     * 1. Team
     * 2. TeamPlayer Count
     * 3. TeamPlayer 목록
     * 4. TransferPost 목록
     *
     * 을 조회한다.
     *
     * 그리고 Tx 안에서 모든 Entity를 DTO로 변환한 뒤
     * OrchestrationService로 반환한다.
     */
    @Test
    @DisplayName("팀 상세 정보에 팀, 선수 목록, 게시물 목록이 함께 반환된다.")
    void getTeamInfo_success() {

        // given
        Team arsenal = saveTeamWithKoreanName(
                "Arsenal",
                "아스널",
                100L,
                LeagueCode.EPL
        );

        Team chelsea = saveTeam(
                "Chelsea",
                200L,
                LeagueCode.EPL
        );


        /*
         * Arsenal 선수 2명
         */
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


        /*
         * 다른 팀 선수.
         *
         * Arsenal 상세 조회에 포함되면 안 된다.
         */
        savePlayerWithTeam(
                "Cole Palmer",
                3L,
                chelsea
        );


        /*
         * Arsenal 게시물 2개
         */
        saveTransferPost(
                TransferPostSource.FABRIZIO_ROMANO,
                "Arsenal transfer post 1",
                "post-1",
                Instant.parse(
                        "2026-10-01T10:00:00Z"
                ),
                true,
                arsenal
        );

        saveTransferPost(
                TransferPostSource.DAVID_ORNSTEIN,
                "Arsenal transfer post 2",
                "post-2",
                Instant.parse(
                        "2026-10-02T10:00:00Z"
                ),
                true,
                arsenal
        );


        /*
         * 다른 팀 게시물.
         *
         * Arsenal 상세 조회에 포함되면 안 된다.
         */
        saveTransferPost(
                TransferPostSource.MATTEO_MORETTO,
                "Chelsea transfer post",
                "post-3",
                Instant.parse(
                        "2026-10-03T10:00:00Z"
                ),
                true,
                chelsea
        );


        // when
        TeamPageInfosResponseDto response =
                teamOrchestrationService.getTeamInfo(
                        arsenal.getId()
                );


        // then

        /*
         * Team 정보
         */
        assertThat(response.getTeams().getTeamId())
                .isEqualTo(
                        arsenal.getId()
                );

        assertThat(response.getTeams().getTeamName())
                .isEqualTo(
                        "Arsenal"
                );

        assertThat(response.getTeams().getTeamNameKo())
                .isEqualTo(
                        "아스널"
                );

        assertThat(response.getTeams().getTeamPlayerCount())
                .isEqualTo(
                        2
                );

        assertThat(response.getTeams().getLogoUrl())
                .isEqualTo(
                        "https://media.api-sports.io/football/teams/100.png"
                );


        /*
         * Player 정보
         */
        assertThat(response.getPlayers().getTeamPlayers())
                .hasSize(2);

        assertThat(response.getPlayers().getTeamPlayers())
                .extracting(
                        player -> player.getPlayerName()
                )
                .containsExactlyInAnyOrder(
                        "Bukayo Saka",
                        "Martin Odegaard"
                );

        assertThat(response.getPlayers().getTeamPlayers())
                .extracting(
                        player -> player.getPlayerName()
                )
                .doesNotContain(
                        "Cole Palmer"
                );


        /*
         * TransferPost 정보
         */
        assertThat(response.getPosts().getPosts())
                .hasSize(2);

        assertThat(response.getPosts().getPosts())
                .extracting(
                        post -> post.getContent()
                )
                .containsExactlyInAnyOrder(
                        "Arsenal transfer post 1",
                        "Arsenal transfer post 2"
                );

        assertThat(response.getPosts().getPosts())
                .extracting(
                        post -> post.getContent()
                )
                .doesNotContain(
                        "Chelsea transfer post"
                );
    }


    /*
     * [신규 - 중요]
     *
     * Team은 존재하지만
     * 선수 / 게시물이 하나도 없는 경우.
     *
     * Team 정보는 정상 반환하고
     * Count = 0
     * Lists = empty
     */
    @Test
    @DisplayName("선수와 게시물이 없는 팀도 정상적으로 상세 조회할 수 있다.")
    void getTeamInfo_withoutPlayersAndPosts() {

        // given
        Team arsenal = saveTeam(
                "Arsenal",
                100L,
                LeagueCode.EPL
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

        assertThat(response.getTeams().getTeamPlayerCount())
                .isZero();

        assertThat(response.getPlayers().getTeamPlayers())
                .isEmpty();

        assertThat(response.getPosts().getPosts())
                .isEmpty();
    }


    /*
     * [신규 - 중요]
     *
     * TeamPlayer의 Player는 LAZY 관계다.
     *
     * Test 자체 Transaction은 없고
     * TeamTxService 내부 Transaction에서
     * DTO 변환이 완료되어야 한다.
     *
     * 선수 한글 이름까지 정상 반환되는지 확인한다.
     */
    @Test
    @DisplayName("팀 상세 조회의 선수 정보는 Tx 안에서 DTO로 변환되어 반환된다.")
    void getTeamInfo_playerDtoMapping() {

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
        TeamPageInfosResponseDto response =
                teamOrchestrationService.getTeamInfo(
                        arsenal.getId()
                );


        // then
        assertThat(response.getPlayers().getTeamPlayers())
                .hasSize(1);

        assertThat(
                response
                        .getPlayers()
                        .getTeamPlayers()
                        .get(0)
                        .getPlayerName()
        )
                .isEqualTo(
                        "부카요 사카"
                );
    }


    /*
     * [실패 - Orchestration]
     */
    @Test
    @DisplayName("팀 상세 조회에서 팀 ID가 null이면 예외가 발생한다.")
    void getTeamInfo_nullTeamId_fail() {

        // given
        Long teamId = null;


        // when & then
        assertThatThrownBy(() ->
                teamOrchestrationService.getTeamInfo(
                        teamId
                )
        )
                .isInstanceOf(
                        InvalidTeamIdException.class
                )
                .hasMessage(
                        "잘못된 TeamId 값입니다."
                );
    }


    /*
     * [실패 - TxService]
     *
     * Orchestration 검증은 통과하지만
     * 실제 DB에 Team이 없는 경우.
     */
    @Test
    @DisplayName("팀 상세 조회에서 존재하지 않는 팀이면 예외가 발생한다.")
    void getTeamInfo_notFound_fail() {

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
                )
                .hasMessage(
                        "존재하지 않는 팀 입니다."
                );
    }


    // ==================================================
    // Fixture
    // ==================================================

    /*
     * 기본 Team 생성.
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
     * 한글 이름이 존재하는 Team 생성.
     *
     * transient 상태에서 한글 이름을 설정한 뒤 저장하므로
     * 별도의 Dirty Checking에 의존하지 않는다.
     */
    private Team saveTeamWithKoreanName(
            String teamName,
            String teamNameKo,
            Long apiFootballId,
            LeagueCode leagueCode
    ) {

        Team team =
                Team.of(
                        teamName,
                        apiFootballId,
                        leagueCode
                );

        team.assignKoTeamName(
                teamNameKo
        );

        return teamRepository.save(
                team
        );
    }


    /*
     * Team에 소속된 Player 생성.
     */
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


    /*
     * 특정 Team에 연결된 TransferPost 생성.
     */
    private TransferPost saveTransferPost(
            TransferPostSource source,
            String content,
            String externalPostId,
            Instant contentCreatedAt,
            boolean transferRelated,
            Team team
    ) {

        TransferPost post =
                TransferPost.of(
                        source,
                        content,
                        externalPostId,
                        contentCreatedAt,
                        transferRelated
                );

        post.assignTeam(
                team
        );

        return transferPostRepository.save(
                post
        );
    }
}