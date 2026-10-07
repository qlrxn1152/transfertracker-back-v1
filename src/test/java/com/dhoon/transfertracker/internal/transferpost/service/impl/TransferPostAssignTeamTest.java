package com.dhoon.transfertracker.internal.transferpost.service.impl;

import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPost;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.transferpost.repository.TransferPostRepository;
import com.dhoon.transfertracker.internal.transferpost.service.translation.TransferPostTranslationServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
@Import({
        TransferPostOrchestrationService.class,
        TransferPostTxService.class
})
@ActiveProfiles("test")

/*
 * 실제 요청 흐름:
 *
 * TransferPostOrchestrationService
 *      → Transaction X
 *      ↓
 * TransferPostTxService
 *      → Transaction 시작
 *      ↓
 * team == null Post 조회
 *      ↓
 * leagueCode != null Team 조회
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
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TransferPostAssignTeamTest {

    @Autowired
    TransferPostOrchestrationService transferPostOrchestrationService;

    @Autowired
    TransferPostRepository transferPostRepository;

    @Autowired
    TeamRepository teamRepository;

    /*
     * 이번 테스트에서는 번역 기능을 사용하지 않는다.
     * OrchestrationService 생성에 필요한 Bean만 Mock 처리한다.
     */
    @MockitoBean
    TransferPostTranslationServiceImpl translationService;


    @AfterEach
    void clearDatabase() {

        // TransferPost → Team FK
        transferPostRepository.deleteAll();
        teamRepository.deleteAll();
    }


    // ==================================================
    // 정상 상황
    // ==================================================

    /*
     * 가장 중요한 테스트.
     *
     * TransferPostTxService에서는
     * post.assignTeam() 이후 repository.save()를 호출하지 않는다.
     *
     * 따라서 Transaction 안에서 조회된 Managed Entity가
     * Dirty Checking으로 UPDATE 되는지 확인한다.
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
                transferPostOrchestrationService.postAssignTeam();


        // then
        assertThat(result)
                .isEqualTo(
                        "OK"
                );


        /*
         * post 객체 자체를 검사하지 않고
         * DB를 다시 조회한다.
         *
         * 실제 Transaction Commit 결과를 검증하기 위함.
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


    /*
     * 현재 구현:
     *
     * post.content.toLowerCase()
     * team.teamName.toLowerCase()
     *
     * 따라서 대소문자는 구분하지 않는다.
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
        transferPostOrchestrationService.postAssignTeam();


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
    // 대상 제한
    // ==================================================

    /*
     * Repository:
     *
     * findAllByTeamIsNull()
     *
     * 이미 team이 존재하는 Post는
     * 아예 assign 대상에서 제외되어야 한다.
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
                         * 내용에는 Arsenal이 있지만
                         * 이미 Chelsea가 할당되어 있다.
                         */
                        "Arsenal are interested in the player.",
                        "post-1"
                );

        post.assignTeam(
                chelsea
        );

        /*
         * 이 assign은 Test Transaction 밖에서 일어났으므로
         * 직접 save해서 기존 상태를 DB에 만들어준다.
         */
        transferPostRepository.save(
                post
        );


        // when
        transferPostOrchestrationService.postAssignTeam();


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


    /*
     * TeamRepository:
     *
     * findAllByLeagueCodeIsNotNull()
     *
     * 따라서 leagueCode가 없는 Team은
     * 게시글 매칭 후보에 포함되지 않는다.
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
        transferPostOrchestrationService.postAssignTeam();


        // then
        assertThat(
                transferPostRepository.findAllByTeamId(
                        unknownTeam.getId()
                )
        )
                .isEmpty();


        /*
         * team이 여전히 null인지도 확인한다.
         *
         * null 관계는 Lazy Loading 문제가 없으므로
         * 이렇게 확인해도 된다.
         */
        TransferPost savedPost =
                transferPostRepository.findById(
                        post.getId()
                )
                        .orElseThrow();

        assertThat(savedPost.getTeam())
                .isNull();
    }


    // ==================================================
    // 경계 상황
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
        transferPostOrchestrationService.postAssignTeam();


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
        transferPostOrchestrationService.postAssignTeam();


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