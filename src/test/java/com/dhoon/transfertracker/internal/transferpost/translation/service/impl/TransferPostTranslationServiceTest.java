package com.dhoon.transfertracker.internal.transferpost.translation.service.impl;

import com.dhoon.transfertracker.external.openai.client.OpenAiClient;
import com.dhoon.transfertracker.external.openai.dto.request.TranslationTarget;
import com.dhoon.transfertracker.external.openai.dto.response.TranslationBatchResult;
import com.dhoon.transfertracker.external.openai.dto.response.TranslationResult;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPost;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.transferpost.domain.TranslateStatus;
import com.dhoon.transfertracker.internal.transferpost.repository.TransferPostRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;


@DataJpaTest
@Import({
        TransferPostTranslationOrchestrationService.class,
        TransferPostTranslationTxService.class
})
@ActiveProfiles("test")

/*
 * 실제 번역 흐름:
 *
 * TransferPostTranslationOrchestrationService
 *      → Transaction X
 *      ↓
 * TransferPostTranslationTxService.findPendingTargets()
 *      → readOnly Transaction 시작
 *      → PENDING 게시글 조회
 *      → TranslationTarget 변환
 *      → Transaction 종료
 *      ↓
 * OpenAiClient.translate()
 *      → Transaction X
 *      → 외부 OpenAI API 호출
 *      ↓
 * 응답 검증
 *      ↓
 * TransferPostTranslationTxService.applyTranslations()
 *      → Transaction 시작
 *      → TransferPost 조회
 *      → transferPost.translate()
 *      → Dirty Checking
 *      → Commit
 *
 *
 * 실제 Transaction 경계를 확인하기 위해
 * DataJpaTest 기본 Transaction을 비활성화한다.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TransferPostTranslationServiceTest {

    @Autowired
    TransferPostTranslationOrchestrationService translationService;

    @Autowired
    TransferPostRepository transferPostRepository;

    @MockitoBean
    OpenAiClient openAiClient;


    @AfterEach
    void clearDatabase() {
        transferPostRepository.deleteAll();
    }


    // ==================================================
    // 정상 번역
    // ==================================================

    @Test
    @DisplayName("PENDING 상태의 게시글을 번역하고 DB에 결과를 반영한다.")
    void translate_success() {

        // given
        TransferPost arsenalPost =
                savePost(
                        "Arsenal have reached an agreement.",
                        "post-1",
                        Instant.parse(
                                "2026-10-01T10:00:00Z"
                        )
                );

        TransferPost chelseaPost =
                savePost(
                        "Chelsea are preparing a new proposal.",
                        "post-2",
                        Instant.parse(
                                "2026-10-02T10:00:00Z"
                        )
                );


        /*
         * 일부러 응답 순서를 요청 순서와 다르게 만든다.
         *
         * Translation 결과 적용은 배열 순서가 아니라
         * postId를 기준으로 이루어져야 한다.
         */
        TranslationBatchResult result =
                TranslationBatchResult.of(
                        List.of(
                                TranslationResult.of(
                                        chelseaPost.getId(),
                                        "첼시가 새로운 제안을 준비하고 있습니다."
                                ),
                                TranslationResult.of(
                                        arsenalPost.getId(),
                                        "아스널이 합의에 도달했습니다."
                                )
                        )
                );


        given(
                openAiClient.translate(
                        anyList()
                )
        )
                .willReturn(
                        result
                );


        // when
        TranslationBatchResult response =
                translationService.translate();


        // then
        assertThat(response.getPosts())
                .hasSize(2);


        TransferPost savedArsenalPost =
                transferPostRepository.findById(
                                arsenalPost.getId()
                        )
                        .orElseThrow();

        TransferPost savedChelseaPost =
                transferPostRepository.findById(
                                chelseaPost.getId()
                        )
                        .orElseThrow();


        assertThat(savedArsenalPost.getTranslatedContent())
                .isEqualTo(
                        "아스널이 합의에 도달했습니다."
                );

        assertThat(savedArsenalPost.getTranslateStatus())
                .isEqualTo(
                        TranslateStatus.COMPLETED
                );

        assertThat(savedArsenalPost.getTranslatedAt())
                .isNotNull();


        assertThat(savedChelseaPost.getTranslatedContent())
                .isEqualTo(
                        "첼시가 새로운 제안을 준비하고 있습니다."
                );

        assertThat(savedChelseaPost.getTranslateStatus())
                .isEqualTo(
                        TranslateStatus.COMPLETED
                );

        assertThat(savedChelseaPost.getTranslatedAt())
                .isNotNull();


        then(openAiClient)
                .should(times(1))
                .translate(
                        anyList()
                );
    }


    // ==================================================
    // PENDING 게시글만 번역
    // ==================================================

    @Test
    @DisplayName("이미 번역 완료된 게시글은 OpenAI 번역 대상에서 제외한다.")
    void translate_onlyPendingPosts() {

        // given
        TransferPost pendingPost =
                savePost(
                        "Arsenal transfer update.",
                        "post-1",
                        Instant.parse(
                                "2026-10-02T10:00:00Z"
                        )
                );


        TransferPost completedPost =
                TransferPost.of(
                        TransferPostSource.FABRIZIO_ROMANO,
                        "Chelsea transfer update.",
                        "post-2",
                        Instant.parse(
                                "2026-10-01T10:00:00Z"
                        ),
                        true
                );

        completedPost.translate(
                "첼시 이적 소식입니다."
        );

        transferPostRepository.save(
                completedPost
        );


        TranslationBatchResult result =
                TranslationBatchResult.of(
                        List.of(
                                TranslationResult.of(
                                        pendingPost.getId(),
                                        "아스널 이적 소식입니다."
                                )
                        )
                );


        given(
                openAiClient.translate(
                        anyList()
                )
        )
                .willReturn(
                        result
                );


        ArgumentCaptor<List<TranslationTarget>> captor =
                ArgumentCaptor.forClass(
                        List.class
                );


        // when
        translationService.translate();


        // then
        then(openAiClient)
                .should(times(1))
                .translate(
                        captor.capture()
                );


        List<TranslationTarget> targets =
                captor.getValue();


        assertThat(targets)
                .hasSize(1);

        assertThat(targets.get(0).getPostId())
                .isEqualTo(
                        pendingPost.getId()
                );

        assertThat(targets.get(0).getContent())
                .isEqualTo(
                        "Arsenal transfer update."
                );





        TransferPost savedCompletedPost =
                transferPostRepository.findById(
                                completedPost.getId()
                        )
                        .orElseThrow();

        assertThat(savedCompletedPost.getTranslatedContent())
                .isEqualTo(
                        "첼시 이적 소식입니다."
                );
    }


    // ==================================================
    // 번역 대상 없음
    // ==================================================

    @Test
    @DisplayName("번역할 PENDING 게시글이 없으면 OpenAI를 호출하지 않고 빈 결과를 반환한다.")
    void translate_noPendingPosts() {

        // given
        TransferPost completedPost =
                TransferPost.of(
                        TransferPostSource.FABRIZIO_ROMANO,
                        "Arsenal transfer update.",
                        "post-1",
                        Instant.parse(
                                "2026-10-01T10:00:00Z"
                        ),
                        true
                );

        completedPost.translate(
                "아스널 이적 소식입니다."
        );

        transferPostRepository.save(
                completedPost
        );


        // when
        TranslationBatchResult response =
                translationService.translate();


        // then
        assertThat(response.getPosts())
                .isEmpty();


        then(openAiClient)
                .should(never())
                .translate(
                        anyList()
                );
    }


    // ==================================================
    // 외부 API / Transaction 경계
    // ==================================================

    /*
     * Translation 구조를 분리한 핵심 이유.
     *
     * OpenAI 응답을 기다리는 동안
     * DB Transaction과 Connection을 유지하면 안 된다.
     */
    @Test
    @DisplayName("OpenAI API를 호출하는 동안 DB 트랜잭션은 활성화되어 있지 않다.")
    void translate_externalApiCall_withoutTransaction() {

        // given
        TransferPost post =
                savePost(
                        "Arsenal transfer update.",
                        "post-1",
                        Instant.parse(
                                "2026-10-01T10:00:00Z"
                        )
                );


        given(
                openAiClient.translate(
                        anyList()
                )
        )
                .willAnswer(invocation -> {

                    assertThat(
                            TransactionSynchronizationManager
                                    .isActualTransactionActive()
                    )
                            .isFalse();


                    return TranslationBatchResult.of(
                            List.of(
                                    TranslationResult.of(
                                            post.getId(),
                                            "아스널 이적 소식입니다."
                                    )
                            )
                    );
                });


        // when
        translationService.translate();


        // then
        TransferPost savedPost =
                transferPostRepository.findById(
                                post.getId()
                        )
                        .orElseThrow();


        assertThat(savedPost.getTranslateStatus())
                .isEqualTo(
                        TranslateStatus.COMPLETED
                );
    }


    // ==================================================
    // 중복 postId 응답
    // ==================================================

    @Test
    @DisplayName("OpenAI 번역 결과에 동일한 postId가 중복되면 예외가 발생하고 DB에 반영하지 않는다.")
    void translate_duplicateResponsePostId() {

        // given
        TransferPost post =
                savePost(
                        "Arsenal transfer update.",
                        "post-1",
                        Instant.parse(
                                "2026-10-01T10:00:00Z"
                        )
                );


        TranslationBatchResult result =
                TranslationBatchResult.of(
                        List.of(
                                TranslationResult.of(
                                        post.getId(),
                                        "아스널 이적 소식입니다."
                                ),
                                TranslationResult.of(
                                        post.getId(),
                                        "중복된 번역 결과입니다."
                                )
                        )
                );


        given(
                openAiClient.translate(
                        anyList()
                )
        )
                .willReturn(
                        result
                );


        // when & then
        assertThatThrownBy(() ->
                translationService.translate()
        )
                .isInstanceOf(
                        IllegalStateException.class
                )
                .hasMessage(
                        "번역 결과에 중복 postId가 존재합니다."
                );


        assertPending(
                post.getId()
        );
    }


    // ==================================================
    // 요청 / 응답 postId 불일치
    // ==================================================

    @Test
    @DisplayName("번역 요청과 응답의 postId가 다르면 예외가 발생하고 DB에 반영하지 않는다.")
    void translate_responsePostIdMismatch() {

        // given
        TransferPost post =
                savePost(
                        "Arsenal transfer update.",
                        "post-1",
                        Instant.parse(
                                "2026-10-01T10:00:00Z"
                        )
                );


        TranslationBatchResult result =
                TranslationBatchResult.of(
                        List.of(
                                TranslationResult.of(
                                        999999L,
                                        "잘못된 게시글의 번역 결과입니다."
                                )
                        )
                );


        given(
                openAiClient.translate(
                        anyList()
                )
        )
                .willReturn(
                        result
                );


        // when & then
        assertThatThrownBy(() ->
                translationService.translate()
        )
                .isInstanceOf(
                        IllegalStateException.class
                )
                .hasMessage(
                        "번역 요청과 응답의 postId가 일치하지 않습니다."
                );


        assertPending(
                post.getId()
        );
    }


    // ==================================================
    // 번역 내용 null
    // ==================================================

    @Test
    @DisplayName("translatedContent가 null이면 예외가 발생하고 DB에 반영하지 않는다.")
    void translate_nullTranslatedContent() {

        // given
        TransferPost post =
                savePost(
                        "Arsenal transfer update.",
                        "post-1",
                        Instant.parse(
                                "2026-10-01T10:00:00Z"
                        )
                );


        TranslationBatchResult result =
                TranslationBatchResult.of(
                        List.of(
                                TranslationResult.of(
                                        post.getId(),
                                        null
                                )
                        )
                );


        given(
                openAiClient.translate(
                        anyList()
                )
        )
                .willReturn(
                        result
                );


        // when & then
        assertThatThrownBy(() ->
                translationService.translate()
        )
                .isInstanceOf(
                        IllegalStateException.class
                )
                .hasMessage(
                        "번역 결과에 비어있는 translatedContent 가 존재합니다."
                );


        assertPending(
                post.getId()
        );
    }


    // ==================================================
    // 번역 내용 Blank
    // ==================================================

    @Test
    @DisplayName("translatedContent가 공백이면 예외가 발생하고 DB에 반영하지 않는다.")
    void translate_blankTranslatedContent() {

        // given
        TransferPost post =
                savePost(
                        "Arsenal transfer update.",
                        "post-1",
                        Instant.parse(
                                "2026-10-01T10:00:00Z"
                        )
                );


        TranslationBatchResult result =
                TranslationBatchResult.of(
                        List.of(
                                TranslationResult.of(
                                        post.getId(),
                                        "   "
                                )
                        )
                );


        given(
                openAiClient.translate(
                        anyList()
                )
        )
                .willReturn(
                        result
                );


        // when & then
        assertThatThrownBy(() ->
                translationService.translate()
        )
                .isInstanceOf(
                        IllegalStateException.class
                )
                .hasMessage(
                        "번역 결과에 비어있는 translatedContent 가 존재합니다."
                );


        assertPending(
                post.getId()
        );
    }


    // ==================================================
    // OpenAI 호출 실패
    // ==================================================

    /*
     * OpenAI 호출 자체가 실패하더라도
     * 아직 DB 변경 Transaction은 시작되지 않았다.
     *
     * 따라서 게시글은 PENDING 상태로 남아 있어야 한다.
     */
    @Test
    @DisplayName("OpenAI 호출이 실패하면 번역 결과를 DB에 반영하지 않는다.")
    void translate_openAiFail_noDatabaseChange() {

        // given
        TransferPost post =
                savePost(
                        "Arsenal transfer update.",
                        "post-1",
                        Instant.parse(
                                "2026-10-01T10:00:00Z"
                        )
                );


        given(
                openAiClient.translate(
                        anyList()
                )
        )
                .willThrow(
                        new RuntimeException(
                                "OpenAI API 호출 실패"
                        )
                );


        // when & then
        assertThatThrownBy(() ->
                translationService.translate()
        )
                .isInstanceOf(
                        RuntimeException.class
                )
                .hasMessage(
                        "OpenAI API 호출 실패"
                );


        assertPending(
                post.getId()
        );
    }


    // ==================================================
    // Fixture
    // ==================================================

    private TransferPost savePost(
            String content,
            String externalPostId,
            Instant contentCreatedAt
    ) {

        return transferPostRepository.save(
                TransferPost.of(
                        TransferPostSource.FABRIZIO_ROMANO,
                        content,
                        externalPostId,
                        contentCreatedAt,
                        true
                )
        );
    }


    private void assertPending(
            Long postId
    ) {

        TransferPost savedPost =
                transferPostRepository.findById(
                                postId
                        )
                        .orElseThrow();


        assertThat(savedPost.getTranslateStatus())
                .isEqualTo(
                        TranslateStatus.PENDING
                );

        assertThat(savedPost.getTranslatedContent())
                .isNull();

        assertThat(savedPost.getTranslatedAt())
                .isNull();
    }
}