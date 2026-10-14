package com.dhoon.transfertracker.internal.transferpost.command.service.impl;

import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
@Import({
        TransferPostCommandOrchestrationService.class,
        TransferPostCommandTxService.class
})
@ActiveProfiles("test")

/*
 * 실제 요청 구조:
 *
 * Client
 *      ↓
 * Controller
 *      ↓
 * TransferPostCommandOrchestrationService
 *      → Transaction X
 *      ↓
 * TransferPostCommandTxService
 *      → @Transactional
 *      ↓
 * team == null인 TransferPost 조회
 *      ↓
 * leagueCode != null인 Team 조회
 *      ↓
 * 게시글 내용과 팀 이름 비교
 *      ↓
 * post.assignTeam(team)
 *      ↓
 * save() 호출 X
 *      ↓
 * Dirty Checking
 *      ↓
 * Transaction Commit
 *      ↓
 * UPDATE transfer_post SET team_id = ?
 *
 *
 * 실제 서비스 Transaction 경계를 검증하기 위해
 * DataJpaTest의 기본 Transaction은 비활성화한다.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TransferPostAssignTeamTest {

    @Autowired
    TransferPostCommandOrchestrationService commandService;

    @Autowired
    TransferPostRepository transferPostRepository;

    @Autowired
    TeamRepository teamRepository;


    @AfterEach
    void clearDatabase() {

        /*
         * TransferPost.team → Team FK가 있으므로
         * 자식인 TransferPost부터 삭제한다.
         */
        transferPostRepository.deleteAll();
        teamRepository.deleteAll();
    }


    // ==================================================
    // 정상 매칭
    // ==================================================

    /*
     * 가장 중요한 테스트.
     *
     * TransferPostCommandTxService에서는
     * post.assignTeam() 이후
     * transferPostRepository.save()를 호출하지 않는다.
     *
     * 따라서 Transaction 내부에서 조회된 Managed Entity가
     * Dirty Checking을 통해 UPDATE 되는지 확인한다.
     */
    @Test
    @DisplayName("게시글 내용에 리그 팀 이름이 포함되면 Dirty Checking으로 팀이 할당된다.")
    void postAssignTeam_success() {

        // given
        Team arsenal =
                saveTeam(
                        "Arsenal",
                        1L,
                        LeagueCode.EPL
                );

        TransferPost post =
                savePost(
                        "Arsenal have agreed personal terms with the player.",
                        "post-1"
                );


        // when
        String result =
                commandService.postAssignTeam();


        // then
        assertThat(result)
                .isEqualTo(
                        "OK"
                );


        /*
         * 기존 post 객체만 확인하지 않고
         * Repository를 통해 DB를 다시 조회한다.
         *
         * 실제 Transaction Commit 결과를 검증하기 위함이다.
         */
        List<TransferPost> arsenalPosts =
                transferPostRepository.findAllByTeamId(
                        arsenal.getId()
                );

        assertThat(arsenalPosts)
                .hasSize(1);

        assertThat(arsenalPosts.get(0).getId())
                .isEqualTo(
                        post.getId()
                );
    }


    // ==================================================
    // 대소문자
    // ==================================================

    /*
     * 현재 구현:
     *
     * post.getContent().toLowerCase()
     * team.getTeamName().toLowerCase()
     *
     * 따라서 팀 이름 매칭 시
     * 대소문자를 구분하지 않는다.
     */
    @Test
    @DisplayName("팀 이름은 대소문자를 구분하지 않고 게시글과 매칭한다.")
    void postAssignTeam_ignoreCase() {

        // given
        Team arsenal =
                saveTeam(
                        "Arsenal",
                        1L,
                        LeagueCode.EPL
                );

        TransferPost post =
                savePost(
                        "ARSENAL are preparing a new proposal.",
                        "post-1"
                );


        // when
        commandService.postAssignTeam();


        // then
        assertThat(
                transferPostRepository.findAllByTeamId(
                        arsenal.getId()
                )
        )
                .extracting(
                        TransferPost::getId
                )
                .containsExactly(
                        post.getId()
                );
    }


    // ==================================================
    // 이미 할당된 게시글
    // ==================================================

    /*
     * CommandTxService에서는:
     *
     * transferPostRepository.findAllByTeamIsNull()
     *
     * 을 사용한다.
     *
     * 따라서 이미 Team이 존재하는 게시글은
     * 애초에 매칭 대상에서 제외되어야 한다.
     */
    @Test
    @DisplayName("이미 팀이 할당된 게시글은 다시 변경하지 않는다.")
    void postAssignTeam_alreadyAssignedPost_notChanged() {

        // given
        Team arsenal =
                saveTeam(
                        "Arsenal",
                        1L,
                        LeagueCode.EPL
                );

        Team chelsea =
                saveTeam(
                        "Chelsea",
                        2L,
                        LeagueCode.EPL
                );


        TransferPost post =
                savePost(
                        /*
                         * 내용에는 Arsenal이 포함되어 있지만
                         * 이미 Chelsea가 할당되어 있다.
                         */
                        "Arsenal are interested in the player.",
                        "post-1"
                );


        post.assignTeam(
                chelsea
        );


        /*
         * 테스트 자체 Transaction이 없기 때문에
         * 여기서는 Fixture 생성 목적으로 직접 저장한다.
         */
        transferPostRepository.save(
                post
        );


        // when
        commandService.postAssignTeam();


        // then
        assertThat(
                transferPostRepository.findAllByTeamId(
                        chelsea.getId()
                )
        )
                .extracting(
                        TransferPost::getId
                )
                .containsExactly(
                        post.getId()
                );


        assertThat(
                transferPostRepository.findAllByTeamId(
                        arsenal.getId()
                )
        )
                .isEmpty();
    }


    // ==================================================
    // League 대상 제한
    // ==================================================

    /*
     * Team 조회:
     *
     * teamRepository.findAllByLeagueCodeIsNotNull()
     *
     * 따라서 leagueCode가 없는 Team은
     * 게시글 매칭 후보에서 제외된다.
     */
    @Test
    @DisplayName("leagueCode가 없는 팀은 게시글 할당 대상에서 제외한다.")
    void postAssignTeam_teamWithoutLeagueCode_notAssigned() {

        // given
        Team unknownTeam =
                saveTeam(
                        "Unknown FC",
                        1L
                );

        TransferPost post =
                savePost(
                        "Unknown FC have completed the transfer.",
                        "post-1"
                );


        // when
        commandService.postAssignTeam();


        // then
        assertThat(
                transferPostRepository.findAllByTeamId(
                        unknownTeam.getId()
                )
        )
                .isEmpty();


        TransferPost savedPost =
                transferPostRepository.findById(
                                post.getId()
                        )
                        .orElseThrow();

        assertThat(savedPost.getTeam())
                .isNull();
    }


    // ==================================================
    // 매칭되는 팀이 없음
    // ==================================================

    @Test
    @DisplayName("게시글에 어떤 리그 팀 이름도 없으면 팀을 할당하지 않는다.")
    void postAssignTeam_noMatchedTeam() {

        // given
        Team arsenal =
                saveTeam(
                        "Arsenal",
                        1L,
                        LeagueCode.EPL
                );

        Team chelsea =
                saveTeam(
                        "Chelsea",
                        2L,
                        LeagueCode.EPL
                );


        TransferPost post =
                savePost(
                        "The player is considering his future.",
                        "post-1"
                );


        // when
        commandService.postAssignTeam();


        // then
        TransferPost savedPost =
                transferPostRepository.findById(
                                post.getId()
                        )
                        .orElseThrow();

        assertThat(savedPost.getTeam())
                .isNull();


        assertThat(
                transferPostRepository.findAllByTeamId(
                        arsenal.getId()
                )
        )
                .isEmpty();

        assertThat(
                transferPostRepository.findAllByTeamId(
                        chelsea.getId()
                )
        )
                .isEmpty();
    }


    // ==================================================
    // 여러 게시글
    // ==================================================

    @Test
    @DisplayName("팀이 할당되지 않은 게시글이 여러 개이면 각각 일치하는 팀을 할당한다.")
    void postAssignTeam_multiplePosts() {

        // given
        Team arsenal =
                saveTeam(
                        "Arsenal",
                        1L,
                        LeagueCode.EPL
                );

        Team chelsea =
                saveTeam(
                        "Chelsea",
                        2L,
                        LeagueCode.EPL
                );


        TransferPost arsenalPost =
                savePost(
                        "Arsenal are preparing an official bid.",
                        "post-1"
                );

        TransferPost chelseaPost =
                savePost(
                        "Chelsea have opened talks with the player.",
                        "post-2"
                );


        // when
        commandService.postAssignTeam();


        // then
        assertThat(
                transferPostRepository.findAllByTeamId(
                        arsenal.getId()
                )
        )
                .extracting(
                        TransferPost::getId
                )
                .containsExactly(
                        arsenalPost.getId()
                );


        assertThat(
                transferPostRepository.findAllByTeamId(
                        chelsea.getId()
                )
        )
                .extracting(
                        TransferPost::getId
                )
                .containsExactly(
                        chelseaPost.getId()
                );
    }


    // ==================================================
    // 매칭 대상이 없음
    // ==================================================

    @Test
    @DisplayName("팀이 할당되지 않은 게시글이 없으면 아무 변경도 발생하지 않는다.")
    void postAssignTeam_noUnassignedPosts() {

        // given
        Team arsenal =
                saveTeam(
                        "Arsenal",
                        1L,
                        LeagueCode.EPL
                );


        TransferPost post =
                savePost(
                        "Arsenal transfer update.",
                        "post-1"
                );

        post.assignTeam(
                arsenal
        );

        transferPostRepository.save(
                post
        );


        // when
        String result =
                commandService.postAssignTeam();


        // then
        assertThat(result)
                .isEqualTo(
                        "OK"
                );

        assertThat(
                transferPostRepository.findAllByTeamId(
                        arsenal.getId()
                )
        )
                .extracting(
                        TransferPost::getId
                )
                .containsExactly(
                        post.getId()
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
            String content,
            String externalPostId
    ) {

        return transferPostRepository.save(
                TransferPost.of(
                        TransferPostSource.FABRIZIO_ROMANO,
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