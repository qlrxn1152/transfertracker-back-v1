package com.dhoon.transfertracker.internal.transferpost.service.impl;

import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.exception.InvalidTeamIdException;
import com.dhoon.transfertracker.internal.team.exception.NotFoundTeamException;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.transfer.dto.response.TransferPostsResponseDto;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPost;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.transferpost.dto.response.TransferPostItemResponseDto;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@DataJpaTest
@Import({
        TransferPostOrchestrationService.class,
        TransferPostTxService.class
})
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TransferPostTeamTest {

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

        /*
         * TransferPost.team → Team FK가 있으므로
         * 자식인 TransferPost부터 삭제한다.
         */
        transferPostRepository.deleteAll();
        teamRepository.deleteAll();
    }


    // ==================================================
    // 정상 상황
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
         * 현재 Test Transaction이 없기 때문에
         * dirty checking을 기대하면 안 된다.
         *
         * assignTeam() 이후 직접 save한다.
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
                transferPostOrchestrationService.getTeamTransferPosts(
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
                transferPostOrchestrationService.getTeamTransferPosts(
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


    /*
     * Team은 실제 DB에 존재하지만
     * TransferPost가 하나도 없는 상황은 정상 상황이다.
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
                transferPostOrchestrationService.getTeamTransferPosts(
                        arsenal.getId()
                );


        // then
        assertThat(response.getPosts())
                .isEmpty();
    }


    /*
     * team이 null인 TransferPost는
     * 특정 팀 조회 결과에 포함되면 안 된다.
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
                transferPostOrchestrationService.getTeamTransferPosts(
                        arsenal.getId()
                );


        // then
        assertThat(response.getPosts())
                .isEmpty();
    }


    // ==================================================
    // 실패 상황
    // ==================================================

    /*
     * null은 DB를 볼 필요가 없는 잘못된 입력값.
     *
     * TransferPostOrchestrationService에서 차단된다.
     */
    @Test
    @DisplayName("teamId가 null이면 팀 게시글을 조회할 수 없다.")
    void getTeamTransferPosts_nullTeamId() {

        // when & then
        assertThatThrownBy(() ->
                transferPostOrchestrationService.getTeamTransferPosts(
                        null
                )
        )
                .isInstanceOf(
                        InvalidTeamIdException.class
                );
    }


    /*
     * Long 값 자체는 정상이다.
     *
     * 하지만 DB에 Team이 없으므로
     * TransferPostTxService에서 예외가 발생한다.
     */
    @Test
    @DisplayName("존재하지 않는 팀의 게시글을 조회하면 예외가 발생한다.")
    void getTeamTransferPosts_teamNotFound() {

        // given
        Long teamId =
                999999L;


        // when & then
        assertThatThrownBy(() ->
                transferPostOrchestrationService.getTeamTransferPosts(
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