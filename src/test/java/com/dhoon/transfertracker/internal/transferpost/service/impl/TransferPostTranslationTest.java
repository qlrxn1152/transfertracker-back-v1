package com.dhoon.transfertracker.internal.transferpost.service.impl;

import com.dhoon.transfertracker.external.openai.client.OpenAiClient;
import com.dhoon.transfertracker.external.openai.dto.request.TranslationTarget;
import com.dhoon.transfertracker.external.openai.dto.response.TranslationBatchResult;
import com.dhoon.transfertracker.external.openai.dto.response.TranslationResult;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPost;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.transferpost.domain.TranslateStatus;
import com.dhoon.transfertracker.internal.transferpost.repository.TransferPostRepository;
import com.dhoon.transfertracker.internal.transferpost.service.translation.TransferPostTranslationServiceImpl;
import com.dhoon.transfertracker.internal.transferpost.service.translation.TransferPostTranslationTxService;
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
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;


@DataJpaTest
@Import({
        TransferPostTranslationServiceImpl.class,
        TransferPostTranslationTxService.class
})
@ActiveProfiles("test")
@Transactional
class TransferPostTranslationTest {

    @Autowired TransferPostTranslationServiceImpl translationService;

    @Autowired TransferPostRepository transferPostRepository;

    @MockitoBean OpenAiClient openAiClient;


    // ==================================================
    // 정상 상황
    // ==================================================

    @Test
    @DisplayName("PENDING 게시물이 존재하면 번역 결과를 DB에 정상적으로 반영한다.")
    void translate_success() {

        // given
        TransferPost post1 = savePost(
                "Manchester United have opened talks with the player.",
                "post-1",
                Instant.parse("2026-10-03T01:00:00Z")
        );

        TransferPost post2 = savePost(
                "Arsenal have submitted a new bid.",
                "post-2",
                Instant.parse("2026-10-03T02:00:00Z")
        );

        TranslationBatchResult openAiResult =
                TranslationBatchResult.of(
                        List.of(
                                TranslationResult.of(
                                        post2.getId(),
                                        "아스널이 새로운 제안을 제출했습니다."
                                ),
                                TranslationResult.of(
                                        post1.getId(),
                                        "맨체스터 유나이티드가 해당 선수와 협상을 시작했습니다."
                                )
                        )
                );

        when(openAiClient.translate(anyList()))
                .thenReturn(openAiResult);


        // when
        TranslationBatchResult result =
                translationService.translate();


        // then
        assertThat(result.getPosts())
                .hasSize(2);

        TransferPost translatedPost1 =
                transferPostRepository.findById(post1.getId())
                        .orElseThrow();

        TransferPost translatedPost2 =
                transferPostRepository.findById(post2.getId())
                        .orElseThrow();


        assertThat(translatedPost1.getTranslateStatus())
                .isEqualTo(TranslateStatus.COMPLETED);

        assertThat(translatedPost1.getTranslatedContent())
                .isEqualTo(
                        "맨체스터 유나이티드가 해당 선수와 협상을 시작했습니다."
                );

        assertThat(translatedPost1.getTranslatedAt())
                .isNotNull();


        assertThat(translatedPost2.getTranslateStatus())
                .isEqualTo(TranslateStatus.COMPLETED);

        assertThat(translatedPost2.getTranslatedContent())
                .isEqualTo(
                        "아스널이 새로운 제안을 제출했습니다."
                );

        assertThat(translatedPost2.getTranslatedAt())
                .isNotNull();
    }


    @Test
    @DisplayName("PENDING 게시물은 contentCreatedAt 기준 최신순으로 OpenAI에 전달한다.")
    void translate_sortByContentCreatedAtDesc() {

        // given
        TransferPost oldest = savePost(
                "old post",
                "post-1",
                Instant.parse("2026-10-01T00:00:00Z")
        );

        TransferPost newest = savePost(
                "new post",
                "post-2",
                Instant.parse("2026-10-03T00:00:00Z")
        );

        TransferPost middle = savePost(
                "middle post",
                "post-3",
                Instant.parse("2026-10-02T00:00:00Z")
        );

        when(openAiClient.translate(anyList()))
                .thenAnswer(invocation -> {

                    List<TranslationTarget> targets =
                            invocation.getArgument(0);

                    return successResult(targets);
                });


        // when
        translationService.translate();


        // then
        ArgumentCaptor<List<TranslationTarget>> captor =
                ArgumentCaptor.forClass(List.class);

        verify(openAiClient)
                .translate(captor.capture());

        assertThat(captor.getValue())
                .extracting(TranslationTarget::getPostId)
                .containsExactly(
                        newest.getId(),
                        middle.getId(),
                        oldest.getId()
                );
    }


    @Test
    @DisplayName("contentCreatedAt이 같으면 id가 큰 게시물부터 OpenAI에 전달한다.")
    void translate_sameCreatedAt_sortByIdDesc() {

        // given
        Instant sameCreatedAt =
                Instant.parse("2026-10-03T00:00:00Z");

        TransferPost first = savePost(
                "first post",
                "post-1",
                sameCreatedAt
        );

        TransferPost second = savePost(
                "second post",
                "post-2",
                sameCreatedAt
        );

        TransferPost third = savePost(
                "third post",
                "post-3",
                sameCreatedAt
        );

        when(openAiClient.translate(anyList()))
                .thenAnswer(invocation -> {

                    List<TranslationTarget> targets =
                            invocation.getArgument(0);

                    return successResult(targets);
                });


        // when
        translationService.translate();


        // then
        ArgumentCaptor<List<TranslationTarget>> captor =
                ArgumentCaptor.forClass(List.class);

        verify(openAiClient)
                .translate(captor.capture());

        assertThat(captor.getValue())
                .extracting(TranslationTarget::getPostId)
                .containsExactly(
                        third.getId(),
                        second.getId(),
                        first.getId()
                );
    }


    @Test
    @DisplayName("OpenAI 응답 순서가 달라도 postId를 기준으로 올바른 게시물에 번역 결과를 반영한다.")
    void translate_responseOrderDifferent_mappingByPostId() {

        // given
        TransferPost post1 = savePost(
                "first content",
                "post-1",
                Instant.parse("2026-10-03T01:00:00Z")
        );

        TransferPost post2 = savePost(
                "second content",
                "post-2",
                Instant.parse("2026-10-03T02:00:00Z")
        );

        /*
         * 실제 요청 순서는
         * post2 -> post1
         *
         * 응답은 반대로
         * post1 -> post2
         */
        TranslationBatchResult result =
                TranslationBatchResult.of(
                        List.of(
                                TranslationResult.of(
                                        post1.getId(),
                                        "첫 번째 게시물 번역"
                                ),
                                TranslationResult.of(
                                        post2.getId(),
                                        "두 번째 게시물 번역"
                                )
                        )
                );

        when(openAiClient.translate(anyList()))
                .thenReturn(result);


        // when
        translationService.translate();


        // then
        TransferPost foundPost1 =
                transferPostRepository.findById(post1.getId())
                        .orElseThrow();

        TransferPost foundPost2 =
                transferPostRepository.findById(post2.getId())
                        .orElseThrow();

        assertThat(foundPost1.getTranslatedContent())
                .isEqualTo("첫 번째 게시물 번역");

        assertThat(foundPost2.getTranslatedContent())
                .isEqualTo("두 번째 게시물 번역");
    }



    @Test
    @DisplayName("OpenAI API를 호출하는 동안에는 DB 트랜잭션이 활성화되어 있지 않다.")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void translate_openAiCall_withoutTransaction() {

        // given
        savePost(
                "Manchester United transfer news",
                "post-1",
                Instant.parse("2026-10-03T00:00:00Z")
        );

        when(openAiClient.translate(anyList()))
                .thenAnswer(invocation -> {

                    /*
                     * DB 조회 Transaction은 이미 종료됐어야 한다.
                     *
                     * 즉 외부 API 호출 시간 동안
                     * DB Transaction을 유지하지 않는 것이 정책.
                     */
                    assertThat(
                            TransactionSynchronizationManager
                                    .isActualTransactionActive()
                    ).isFalse();

                    List<TranslationTarget> targets =
                            invocation.getArgument(0);

                    return successResult(targets);
                });


        // when
        translationService.translate();


        // then
        verify(openAiClient, times(1))
                .translate(anyList());

        // clear
        transferPostRepository.deleteAll();
    }


    // ==================================================
    // 예외 상황
    // ==================================================

    @Test
    @DisplayName("OpenAI API 호출에 실패하면 게시물은 PENDING 상태로 유지한다.")
    void translate_openAiFail_keepPending() {

        // given
        TransferPost post = savePost(
                "Manchester United transfer news",
                "post-1",
                Instant.parse("2026-10-03T00:00:00Z")
        );

        when(openAiClient.translate(anyList()))
                .thenThrow(
                        new RuntimeException("OpenAI API 호출 실패")
                );


        // when & then
        assertThatThrownBy(() ->
                translationService.translate()
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessage("OpenAI API 호출 실패");


        TransferPost found =
                transferPostRepository.findById(post.getId())
                        .orElseThrow();

        assertThat(found.getTranslateStatus())
                .isEqualTo(TranslateStatus.PENDING);

        assertThat(found.getTranslatedContent())
                .isNull();

        assertThat(found.getTranslatedAt())
                .isNull();
    }


    @Test
    @DisplayName("OpenAI 응답에 중복 postId가 존재하면 번역 결과를 DB에 반영하지 않는다.")
    void translate_duplicateResponsePostId_fail() {

        // given
        TransferPost post1 = savePost(
                "first post",
                "post-1",
                Instant.parse("2026-10-03T01:00:00Z")
        );

        TransferPost post2 = savePost(
                "second post",
                "post-2",
                Instant.parse("2026-10-03T02:00:00Z")
        );

        TranslationBatchResult duplicatedResult =
                TranslationBatchResult.of(
                        List.of(
                                TranslationResult.of(
                                        post1.getId(),
                                        "첫 번째 번역"
                                ),
                                TranslationResult.of(
                                        post1.getId(),
                                        "중복된 번역"
                                )
                        )
                );

        when(openAiClient.translate(anyList()))
                .thenReturn(duplicatedResult);


        // when & then
        assertThatThrownBy(() ->
                translationService.translate()
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "번역 결과에 중복 postId가 존재합니다."
                );


        assertPending(post1.getId());
        assertPending(post2.getId());
    }


    @Test
    @DisplayName("요청하지 않은 postId가 응답에 포함되면 번역 결과를 DB에 반영하지 않는다.")
    void translate_unknownResponsePostId_fail() {

        // given
        TransferPost post1 = savePost(
                "first post",
                "post-1",
                Instant.parse("2026-10-03T01:00:00Z")
        );

        TransferPost post2 = savePost(
                "second post",
                "post-2",
                Instant.parse("2026-10-03T02:00:00Z")
        );

        TranslationBatchResult wrongResult =
                TranslationBatchResult.of(
                        List.of(
                                TranslationResult.of(
                                        post1.getId(),
                                        "첫 번째 번역"
                                ),
                                TranslationResult.of(
                                        999999L,
                                        "잘못된 번역"
                                )
                        )
                );

        when(openAiClient.translate(anyList()))
                .thenReturn(wrongResult);


        // when & then
        assertThatThrownBy(() ->
                translationService.translate()
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "번역 요청과 응답의 postId가 일치하지 않습니다."
                );


        assertPending(post1.getId());
        assertPending(post2.getId());
    }


    @Test
    @DisplayName("OpenAI 응답에서 일부 postId가 누락되면 번역 결과를 DB에 반영하지 않는다.")
    void translate_missingResponsePostId_fail() {

        // given
        TransferPost post1 = savePost(
                "first post",
                "post-1",
                Instant.parse("2026-10-03T01:00:00Z")
        );

        TransferPost post2 = savePost(
                "second post",
                "post-2",
                Instant.parse("2026-10-03T02:00:00Z")
        );

        TranslationBatchResult missingResult =
                TranslationBatchResult.of(
                        List.of(
                                TranslationResult.of(
                                        post1.getId(),
                                        "첫 번째 번역"
                                )
                        )
                );

        when(openAiClient.translate(anyList()))
                .thenReturn(missingResult);


        // when & then
        assertThatThrownBy(() ->
                translationService.translate()
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "번역 요청과 응답의 postId가 일치하지 않습니다."
                );


        assertPending(post1.getId());
        assertPending(post2.getId());
    }


    // ==================================================
    // 경계 상황
    // ==================================================

    @Test
    @DisplayName("PENDING 게시물이 없으면 OpenAI API를 호출하지 않고 빈 결과를 반환한다.")
    void translate_noPendingPosts_returnEmpty() {

        // given
        // 번역할 게시물이 없음


        // when
        TranslationBatchResult result =
                translationService.translate();


        // then
        assertThat(result.getPosts())
                .isEmpty();

        verifyNoInteractions(openAiClient);
    }


    @Test
    @DisplayName("COMPLETED 게시물은 번역 대상에 포함하지 않는다.")
    void translate_completedPost_excluded() {

        // given
        TransferPost completedPost = savePost(
                "already translated",
                "post-1",
                Instant.parse("2026-10-03T00:00:00Z")
        );

        completedPost.translate("이미 번역된 게시물");

        transferPostRepository.save(completedPost);


        // when
        TranslationBatchResult result =
                translationService.translate();


        // then
        assertThat(result.getPosts())
                .isEmpty();

        verifyNoInteractions(openAiClient);
    }


    @Test
    @DisplayName("FAILED 게시물은 현재 자동 번역 대상에 포함하지 않는다.")
    void translate_failedPost_excluded() {

        // given
        TransferPost failedPost = savePost(
                "failed post",
                "post-1",
                Instant.parse("2026-10-03T00:00:00Z")
        );

        failedPost.failTranslate();

        transferPostRepository.save(failedPost);


        // when
        TranslationBatchResult result =
                translationService.translate();


        // then
        assertThat(result.getPosts())
                .isEmpty();

        verifyNoInteractions(openAiClient);
    }


    @Test
    @DisplayName("PENDING 게시물이 정확히 20개이면 20개 모두 번역 대상으로 전달한다.")
    void translate_exact20() {

        // given
        IntStream.rangeClosed(1, 20)
                .forEach(i ->
                        savePost(
                                "content-" + i,
                                "external-" + i,
                                Instant.parse(
                                                "2026-10-01T00:00:00Z"
                                        )
                                        .plusSeconds(i)
                        )
                );

        when(openAiClient.translate(anyList()))
                .thenAnswer(invocation -> {

                    List<TranslationTarget> targets =
                            invocation.getArgument(0);

                    return successResult(targets);
                });


        // when
        translationService.translate();


        // then
        ArgumentCaptor<List<TranslationTarget>> captor =
                ArgumentCaptor.forClass(List.class);

        verify(openAiClient)
                .translate(captor.capture());

        assertThat(captor.getValue())
                .hasSize(20);
    }


    @Test
    @DisplayName("PENDING 게시물이 20개를 초과하면 최신 20개만 번역 대상으로 전달한다.")
    void translate_over20_onlyLatest20() {

        // given
        List<TransferPost> posts =
                IntStream.rangeClosed(1, 21)
                        .mapToObj(i ->
                                savePost(
                                        "content-" + i,
                                        "external-" + i,
                                        Instant.parse(
                                                        "2026-10-01T00:00:00Z"
                                                )
                                                .plusSeconds(i)
                                )
                        )
                        .toList();

        TransferPost oldestPost =
                posts.get(0);

        when(openAiClient.translate(anyList()))
                .thenAnswer(invocation -> {

                    List<TranslationTarget> targets =
                            invocation.getArgument(0);

                    return successResult(targets);
                });


        // when
        translationService.translate();


        // then
        ArgumentCaptor<List<TranslationTarget>> captor =
                ArgumentCaptor.forClass(List.class);

        verify(openAiClient)
                .translate(captor.capture());

        List<TranslationTarget> requestedTargets =
                captor.getValue();

        assertThat(requestedTargets)
                .hasSize(20);

        assertThat(requestedTargets)
                .extracting(TranslationTarget::getPostId)
                .doesNotContain(oldestPost.getId());


        /*
         * 가장 오래된 게시물은 이번 Batch에 포함되지 않았으므로
         * PENDING 상태여야 한다. 최신것들 기준으로 20개이므로.
         */
        TransferPost oldest =
                transferPostRepository.findById(
                                oldestPost.getId()
                        )
                        .orElseThrow();

        assertThat(oldest.getTranslateStatus())
                .isEqualTo(TranslateStatus.PENDING);

        assertThat(oldest.getTranslatedContent())
                .isNull();
    }

    @Test
    @DisplayName("번역 결과에 빈 번역문이 존재하면 전체 번역 결과를 DB에 반영하지 않는다.")
    void translate_blankTranslatedContent_fail() {

        // given
        TransferPost post1 = savePost(
                "first post",
                "post-1",
                Instant.parse("2026-10-03T01:00:00Z")
        );

        TransferPost post2 = savePost(
                "second post",
                "post-2",
                Instant.parse("2026-10-03T02:00:00Z")
        );

        TranslationBatchResult invalidResult =
                TranslationBatchResult.of(
                        List.of(
                                TranslationResult.of(
                                        post1.getId(),
                                        "첫 번째 번역"
                                ),
                                TranslationResult.of(
                                        post2.getId(),
                                        ""
                                )
                        )
                );

        when(openAiClient.translate(anyList()))
                .thenReturn(invalidResult);


        // when & then
        assertThatThrownBy(() ->
                translationService.translate()
        )
                .isInstanceOf(IllegalStateException.class);


        assertPending(post1.getId());
        assertPending(post2.getId());
    }


    // ==================================================
    // Fixture
    // ==================================================

    private TransferPost savePost(
            String content,
            String externalPostId,
            Instant createdAt
    ) {

        return transferPostRepository.save(
                TransferPost.of(
                        TransferPostSource.FABRIZIO_ROMANO,
                        content,
                        externalPostId,
                        createdAt,
                        true
                )
        );
    }


    private TranslationBatchResult successResult(
            List<TranslationTarget> targets
    ) {

        List<TranslationResult> results =
                targets.stream()
                        .map(target ->
                                TranslationResult.of(
                                        target.getPostId(),
                                        "translated-" +
                                                target.getPostId()
                                )
                        )
                        .toList();

        return TranslationBatchResult.of(results);
    }


    private void assertPending(Long postId) {

        TransferPost post =
                transferPostRepository.findById(postId)
                        .orElseThrow();

        assertThat(post.getTranslateStatus())
                .isEqualTo(TranslateStatus.PENDING);

        assertThat(post.getTranslatedContent())
                .isNull();

        assertThat(post.getTranslatedAt())
                .isNull();
    }
}