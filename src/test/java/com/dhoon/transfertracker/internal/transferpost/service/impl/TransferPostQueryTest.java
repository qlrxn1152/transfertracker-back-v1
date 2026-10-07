package com.dhoon.transfertracker.internal.transferpost.service.impl;

import com.dhoon.transfertracker.internal.transfer.dto.response.TransferPostsResponseDto;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPost;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.transferpost.dto.response.TransferPostItemResponseDto;
import com.dhoon.transfertracker.internal.transferpost.exception.InvalidSourcerException;
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

/*
 * 실제 요청 구조:
 *
 * Client
 *      ↓
 * Controller
 *      ↓
 * TransferPostOrchestrationService
 *      → Transaction X
 *      → 입력 검증
 *      ↓
 * TransferPostTxService
 *      → @Transactional(readOnly = true)
 *      → Repository 조회
 *      → DTO 변환
 *      ↓
 * Transaction 종료
 *      ↓
 * DTO 반환
 *
 *
 * 실제 Transaction 경계를 검증하기 위해
 * DataJpaTest 기본 Transaction을 사용하지 않는다.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TransferPostQueryTest {

    @Autowired
    TransferPostOrchestrationService transferPostOrchestrationService;

    @Autowired
    TransferPostRepository transferPostRepository;


    /*
     * 현재 Query 테스트에서는 번역 기능을 사용하지 않는다.
     *
     * 하지만 TransferPostOrchestrationService 생성자에
     * TranslationService가 필요하므로 Bean만 Mock 처리한다.
     */
    @MockitoBean
    TransferPostTranslationServiceImpl translationService;


    @AfterEach
    void clearDatabase() {
        transferPostRepository.deleteAll();
    }


    // ==================================================
    // Sourcer 조회
    // ==================================================

    @Test
    @DisplayName("특정 소스의 이적 게시글만 조회한다.")
    void getSourcerAllPosts_success() {

        // given
        savePost(
                TransferPostSource.FABRIZIO_ROMANO,
                "Arsenal have reached an agreement.",
                "post-1",
                Instant.parse("2026-10-01T10:00:00Z"),
                true
        );

        savePost(
                TransferPostSource.FABRIZIO_ROMANO,
                "Chelsea are interested in the player.",
                "post-2",
                Instant.parse("2026-10-02T10:00:00Z"),
                true
        );

        savePost(
                TransferPostSource.DAVID_ORNSTEIN,
                "Manchester United update.",
                "post-3",
                Instant.parse("2026-10-03T10:00:00Z"),
                true
        );


        // when
        TransferPostsResponseDto response =
                transferPostOrchestrationService.getSourcerAllPosts(
                        TransferPostSource.FABRIZIO_ROMANO
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

        assertThat(response.getPosts())
                .extracting(
                        TransferPostItemResponseDto::getSource
                )
                .containsOnly(
                        TransferPostSource.FABRIZIO_ROMANO
                );
    }


    @Test
    @DisplayName("해당 소스의 게시글이 없으면 빈 리스트를 반환한다.")
    void getSourcerAllPosts_empty() {

        // given
        savePost(
                TransferPostSource.DAVID_ORNSTEIN,
                "David Ornstein post",
                "post-1",
                Instant.parse("2026-10-01T10:00:00Z"),
                true
        );


        // when
        TransferPostsResponseDto response =
                transferPostOrchestrationService.getSourcerAllPosts(
                        TransferPostSource.FABRIZIO_ROMANO
                );


        // then
        assertThat(response.getPosts())
                .isEmpty();
    }


    /*
     * sourcer == null은 DB를 확인할 문제가 아니다.
     *
     * 따라서 Orchestration에서 차단되어야 한다.
     */
    @Test
    @DisplayName("sourcer가 null이면 예외가 발생한다.")
    void getSourcerAllPosts_nullSourcer() {

        // when & then
        assertThatThrownBy(() ->
                transferPostOrchestrationService.getSourcerAllPosts(
                        null
                )
        )
                .isInstanceOf(
                        InvalidSourcerException.class
                );
    }


    // ==================================================
    // 전체 조회
    // ==================================================

    @Test
    @DisplayName("모든 소스의 이적 게시글을 조회한다.")
    void getAllTransferPosts_success() {

        // given
        savePost(
                TransferPostSource.FABRIZIO_ROMANO,
                "Fabrizio post",
                "post-1",
                Instant.parse("2026-10-01T10:00:00Z"),
                true
        );

        savePost(
                TransferPostSource.DAVID_ORNSTEIN,
                "Ornstein post",
                "post-2",
                Instant.parse("2026-10-02T10:00:00Z"),
                false
        );

        savePost(
                TransferPostSource.MATTEO_MORETTO,
                "Moretto post",
                "post-3",
                Instant.parse("2026-10-03T10:00:00Z"),
                true
        );


        // when
        TransferPostsResponseDto response =
                transferPostOrchestrationService.getAllTransferPosts();


        // then
        assertThat(response.getPosts())
                .hasSize(3);

        assertThat(response.getPosts())
                .extracting(
                        TransferPostItemResponseDto::getExternalPostId
                )
                .containsExactlyInAnyOrder(
                        "post-1",
                        "post-2",
                        "post-3"
                );
    }


    @Test
    @DisplayName("게시글이 하나도 없으면 빈 리스트를 반환한다.")
    void getAllTransferPosts_empty() {

        // when
        TransferPostsResponseDto response =
                transferPostOrchestrationService.getAllTransferPosts();


        // then
        assertThat(response.getPosts())
                .isEmpty();
    }


    // ==================================================
    // DTO Mapping
    // ==================================================

    @Test
    @DisplayName("TransferPost의 정보가 응답 DTO에 정상적으로 매핑된다.")
    void getAllTransferPosts_dtoMapping() {

        // given
        Instant createdAt =
                Instant.parse(
                        "2026-10-01T10:00:00Z"
                );

        TransferPost post =
                savePost(
                        TransferPostSource.FABRIZIO_ROMANO,
                        "Arsenal have reached an agreement.",
                        "external-123",
                        createdAt,
                        true
                );


        // when
        TransferPostsResponseDto response =
                transferPostOrchestrationService.getAllTransferPosts();


        // then
        assertThat(response.getPosts())
                .hasSize(1);

        TransferPostItemResponseDto item =
                response.getPosts().get(0);

        assertThat(item.getPostId())
                .isEqualTo(
                        post.getId()
                );

        assertThat(item.getExternalPostId())
                .isEqualTo(
                        "external-123"
                );

        assertThat(item.getContent())
                .isEqualTo(
                        "Arsenal have reached an agreement."
                );

        assertThat(item.getSource())
                .isEqualTo(
                        TransferPostSource.FABRIZIO_ROMANO
                );

        assertThat(item.getPostCreatedAt())
                .isEqualTo(
                        createdAt
                );

        assertThat(item.isRelateTransfer())
                .isTrue();

        assertThat(item.getTranslatedContent())
                .isNull();
    }


    /*
     * translatedContent가 존재하는 경우에도
     * DTO에 정상적으로 포함되는지 확인한다.
     */
    @Test
    @DisplayName("번역된 게시글은 translatedContent를 응답에 포함한다.")
    void getAllTransferPosts_withTranslatedContent() {

        // given
        TransferPost post =
                TransferPost.of(
                        TransferPostSource.FABRIZIO_ROMANO,
                        "Arsenal have reached an agreement.",
                        "post-1",
                        Instant.parse("2026-10-01T10:00:00Z"),
                        true
                );

        post.translate(
                "아스널이 합의에 도달했습니다."
        );

        transferPostRepository.save(
                post
        );


        // when
        TransferPostsResponseDto response =
                transferPostOrchestrationService.getAllTransferPosts();


        // then
        TransferPostItemResponseDto item =
                response.getPosts().get(0);

        assertThat(item.getTranslatedContent())
                .isEqualTo(
                        "아스널이 합의에 도달했습니다."
                );
    }


    // ==================================================
    // Fixture
    // ==================================================

    private TransferPost savePost(
            TransferPostSource source,
            String content,
            String externalPostId,
            Instant contentCreatedAt,
            boolean transferRelated
    ) {

        return transferPostRepository.save(
                TransferPost.of(
                        source,
                        content,
                        externalPostId,
                        contentCreatedAt,
                        transferRelated
                )
        );
    }
}