package com.dhoon.transfertracker.internal.transferpost.query.service.impl;

import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.exception.InvalidTeamIdException;
import com.dhoon.transfertracker.internal.team.exception.NotFoundTeamException;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPost;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.transferpost.dto.response.TransferPostItemResponseDto;
import com.dhoon.transfertracker.internal.transferpost.dto.response.TransferPostsResponseDto;
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
        TransferPostQueryOrchestrationService.class,
        TransferPostQueryTxService.class
})
@ActiveProfiles("test")

/*
 * 실제 요청 구조:
 *
 * Client
 *      ↓
 * Controller
 *      ↓
 * TransferPostQueryOrchestrationService
 *      → teamId 입력값 검증
 *      → Transaction X
 *      ↓
 * TransferPostQueryTxService
 *      → @Transactional(readOnly = true)
 *      ↓
 * Team 존재 여부 확인
 *      ↓
 * TransferPostRepository 조회
 *      ↓
 * DTO 변환
 *      ↓
 * Transaction 종료
 *
 *
 * DataJpaTest의 기본 Transaction을 비활성화한다.
 *
 * 실제 서비스의 Transaction 경계를 그대로 검증하기 위함이다.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TransferPostTeamQueryTest {

    @Autowired
    TransferPostQueryOrchestrationService queryService;

    @Autowired
    TransferPostRepository transferPostRepository;

    @Autowired
    TeamRepository teamRepository;


    @AfterEach
    void clearDatabase() {

        /*
         * TransferPost.team → Team FK
         *
         * 따라서 자식인 TransferPost를 먼저 삭제한다.
         */
        transferPostRepository.deleteAll();
        teamRepository.deleteAll();
    }


    // ==================================================
    // 정상 조회
    // ==================================================

    @Test
    @DisplayName("특정 팀에 할당된 게시글만 조회할 수 있다.")
    void getTeamTransferPosts_success() {

        // given
        Team arsenal =
                saveTeam(
                        "Arsenal",
                        1L
                );

        Team chelsea =
                saveTeam(
                        "Chelsea",
                        2L
                );


        TransferPost arsenalPost1 =
                savePost(
                        TransferPostSource.FABRIZIO_ROMANO,
                        "Arsenal transfer update.",
                        "post-1"
                );

        TransferPost arsenalPost2 =
                savePost(
                        TransferPostSource.DAVID_ORNSTEIN,
                        "Arsenal are interested in the player.",
                        "post-2"
                );

        TransferPost chelseaPost =
                savePost(
                        TransferPostSource.FABRIZIO_ROMANO,
                        "Chelsea transfer update.",
                        "post-3"
                );


        arsenalPost1.assignTeam(
                arsenal
        );

        arsenalPost2.assignTeam(
                arsenal
        );

        chelseaPost.assignTeam(
                chelsea
        );


        /*
         * 테스트 자체 Transaction이 없기 때문에 ( 현재 테스트는 조회용 )
         * Dirty Checking을 기대하면 안 된다.
         *
         * 테스트 Fixture를 만드는 과정이므로
         * 변경된 Entity를 직접 저장한다.
         */
        transferPostRepository.save(
                arsenalPost1
        );

        transferPostRepository.save(
                arsenalPost2
        );

        transferPostRepository.save(
                chelseaPost
        );


        // when
        TransferPostsResponseDto response =
                queryService.getTeamTransferPosts(
                        arsenal.getId()
                );


        // then
        assertThat(response.getPosts())
                .hasSize(2);

        assertThat(response.getPosts())
                .extracting(
                        TransferPostItemResponseDto::getExternalPostId
                )
                .containsExactlyInAnyOrder(
                        "post-1",
                        "post-2"
                );
    }


    @Test
    @DisplayName("다른 팀에 할당된 게시글은 조회하지 않는다.")
    void getTeamTransferPosts_excludeOtherTeam() {

        // given
        Team arsenal =
                saveTeam(
                        "Arsenal",
                        1L
                );

        Team chelsea =
                saveTeam(
                        "Chelsea",
                        2L
                );


        TransferPost arsenalPost =
                savePost(
                        TransferPostSource.FABRIZIO_ROMANO,
                        "Arsenal transfer update.",
                        "post-1"
                );

        TransferPost chelseaPost =
                savePost(
                        TransferPostSource.FABRIZIO_ROMANO,
                        "Chelsea transfer update.",
                        "post-2"
                );


        arsenalPost.assignTeam(
                arsenal
        );

        chelseaPost.assignTeam(
                chelsea
        );


        transferPostRepository.save(
                arsenalPost
        );

        transferPostRepository.save(
                chelseaPost
        );


        // when
        TransferPostsResponseDto response =
                queryService.getTeamTransferPosts(
                        arsenal.getId()
                );


        // then
        assertThat(response.getPosts())
                .hasSize(1);

        assertThat(response.getPosts().get(0).getExternalPostId())
                .isEqualTo(
                        "post-1"
                );
    }


    // ==================================================
    // 빈 조회
    // ==================================================

    /*
     * Team 자체는 존재하지만
     * 해당 Team에 연결된 TransferPost가 없는 경우다.
     *
     * 이것은 예외 상황이 아니라 정상적인 빈 조회다.
     */
    @Test
    @DisplayName("팀은 존재하지만 게시글이 없으면 빈 리스트를 반환한다.")
    void getTeamTransferPosts_empty() {

        // given
        Team arsenal =
                saveTeam(
                        "Arsenal",
                        1L
                );


        // when
        TransferPostsResponseDto response =
                queryService.getTeamTransferPosts(
                        arsenal.getId()
                );


        // then
        assertThat(response.getPosts())
                .isEmpty();
    }


    /*
     * 아직 Team이 연결되지 않은 TransferPost는
     * 특정 Team 조회 결과에 포함되어서는 안 된다.
     */
    @Test
    @DisplayName("아직 팀이 할당되지 않은 게시글은 특정 팀 조회 결과에 포함하지 않는다.")
    void getTeamTransferPosts_excludeUnassignedPost() {

        // given
        Team arsenal =
                saveTeam(
                        "Arsenal",
                        1L
                );


        savePost(
                TransferPostSource.FABRIZIO_ROMANO,
                "Arsenal transfer update.",
                "post-1"
        );


        // when
        TransferPostsResponseDto response =
                queryService.getTeamTransferPosts(
                        arsenal.getId()
                );


        // then
        assertThat(response.getPosts())
                .isEmpty();
    }


    // ==================================================
    // 잘못된 입력값
    // ==================================================

    /*
     * teamId == null은 DB를 조회할 필요가 없다.
     *
     * QueryOrchestrationService에서
     * TxService 진입 전에 차단되어야 한다.
     */
    @Test
    @DisplayName("teamId가 null이면 팀 게시글을 조회할 수 없다.")
    void getTeamTransferPosts_nullTeamId() {

        // when & then
        assertThatThrownBy(() ->
                queryService.getTeamTransferPosts(
                        null
                )
        )
                .isInstanceOf(
                        InvalidTeamIdException.class
                );
    }


    // ==================================================
    // 존재하지 않는 Team
    // ==================================================

    /*
     * teamId라는 입력값 자체는 정상이다.
     *
     * 따라서 Orchestration 검증은 통과한다.
     *
     * 하지만 실제 DB에 해당 Team이 존재하지 않으므로
     * QueryTxService 내부에서 예외가 발생해야 한다.
     */
    @Test
    @DisplayName("존재하지 않는 팀의 게시글을 조회하면 예외가 발생한다.")
    void getTeamTransferPosts_teamNotFound() {

        // given
        Long teamId =
                999999L;


        // when & then
        assertThatThrownBy(() ->
                queryService.getTeamTransferPosts(
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

        return teamRepository.save(
                Team.of(
                        teamName,
                        apiFootballId
                )
        );
    }


    private TransferPost savePost(
            TransferPostSource source,
            String content,
            String externalPostId
    ) {

        return transferPostRepository.save(
                TransferPost.of(
                        source,
                        content,
                        externalPostId,
                        Instant.parse(
                                "2026-10-01T10:00:00Z"
                        ),
                        true
                )
        );
    }
}