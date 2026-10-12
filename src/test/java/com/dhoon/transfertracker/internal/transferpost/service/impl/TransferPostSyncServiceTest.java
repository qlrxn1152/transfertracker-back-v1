package com.dhoon.transfertracker.internal.transferpost.sync.service.impl;

import com.dhoon.transfertracker.external.x.client.XClient;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPost;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.transferpost.dto.response.TransferPostItemResponseDto;
import com.dhoon.transfertracker.internal.transferpost.dto.response.TransferPostsResponseDto;
import com.dhoon.transfertracker.internal.transferpost.exception.InvalidSourcerException;
import com.dhoon.transfertracker.internal.transferpost.repository.TransferPostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;


@DataJpaTest
@Import({
        TransferPostSyncServiceImpl.class,
        TransferPostSyncTxServiceImpl.class
})
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TransferPostSyncServiceTest {

    @Autowired
    TransferPostSyncServiceImpl transferPostSyncService;

    @Autowired
    TransferPostRepository transferPostRepository;

    /*
     * 실제 X API는 호출하지 않는다.
     *
     * XClient 자체가 이번 테스트의 대상이 아니라
     *
     * X 응답
     *     ↓
     * SyncService
     *     ↓
     * 게시물별 Transaction
     *
     * 흐름을 테스트한다.
     */
    @MockitoBean
    XClient xClient;


    private final ObjectMapper objectMapper = new ObjectMapper();


    @BeforeEach
    void clearDatabase() {

        /*
         * 테스트 자체 Transaction을 비활성화했기 때문에
         * 테스트 종료 후 자동 Rollback되지 않는다.
         */
        transferPostRepository.deleteAll();
    }


    // ==================================================
    // 정상 상황
    // ==================================================


    @Test
    @DisplayName(
            "X에서 가져온 여러 게시물을 저장하고 DTO를 반환한다."
    )
    void syncPosts_success() {

        // given
        JsonNode response =
                normalResponse();


        given(
                xClient.callExternalApi(
                        TransferPostSource.FABRIZIO_ROMANO
                )
        )
                .willReturn(
                        response
                );


        // when
        TransferPostsResponseDto result =
                transferPostSyncService.syncPosts(
                        TransferPostSource.FABRIZIO_ROMANO
                );


        // then
        assertThat(result.getPosts())
                .hasSize(2);


        assertThat(result.getPosts())
                .extracting(
                        TransferPostItemResponseDto::getExternalPostId
                )
                .containsExactly(
                        "1001",
                        "1002"
                );


        assertThat(transferPostRepository.count())
                .isEqualTo(2);


        List<TransferPost> savedPosts =
                transferPostRepository.findAll();


        assertThat(savedPosts)
                .extracting(
                        TransferPost::getExternalPostId
                )
                .containsExactlyInAnyOrder(
                        "1001",
                        "1002"
                );


        then(xClient)
                .should(times(1))
                .callExternalApi(
                        TransferPostSource.FABRIZIO_ROMANO
                );
    }


    // ==================================================
    // 이적 관련 여부
    // ==================================================


    @Test
    @DisplayName(
            "이적 관련 키워드가 포함된 게시물만 이적 관련 게시물로 판단한다."
    )
    void syncPosts_transferRelated() {

        // given
        JsonNode response =
                readJson(
                        """
                        {
                          "data": [
                            {
                              "id": "1001",
                              "text": "Arsenal have reached an agreement for the transfer.",
                              "created_at": "2026-10-12T00:00:00Z"
                            },
                            {
                              "id": "1002",
                              "text": "Training session completed today.",
                              "created_at": "2026-10-12T00:01:00Z"
                            }
                          ]
                        }
                        """
                );


        given(
                xClient.callExternalApi(
                        TransferPostSource.FABRIZIO_ROMANO
                )
        )
                .willReturn(
                        response
                );


        // when
        transferPostSyncService.syncPosts(
                TransferPostSource.FABRIZIO_ROMANO
        );


        // then
        TransferPost transferPost =
                transferPostRepository
                        .findByExternalPostId(
                                "1001"
                        )
                        .orElseThrow();


        TransferPost normalPost =
                transferPostRepository
                        .findByExternalPostId(
                                "1002"
                        )
                        .orElseThrow();


        assertThat(transferPost.isTransferRelated())
                .isTrue();


        assertThat(normalPost.isTransferRelated())
                .isFalse();
    }


    // ==================================================
    // 이미지
    // ==================================================


    @Test
    @DisplayName(
            "게시물에 사진 media key가 존재하면 사진 URL도 함께 저장한다."
    )
    void syncPosts_photoUrl() {

        // given
        JsonNode response =
                responseWithPhoto();


        given(
                xClient.callExternalApi(
                        TransferPostSource.FABRIZIO_ROMANO
                )
        )
                .willReturn(
                        response
                );


        // when
        transferPostSyncService.syncPosts(
                TransferPostSource.FABRIZIO_ROMANO
        );


        // then
        TransferPost savedPost =
                transferPostRepository
                        .findByExternalPostId(
                                "1001"
                        )
                        .orElseThrow();


        assertThat(savedPost.getPhotoUrl())
                .isEqualTo(
                        "https://pbs.twimg.com/media/test-image.jpg"
                );
    }


    // ==================================================
    // 외부 API / Transaction 경계
    // ==================================================


    /*
     * 이번 리팩토링의 핵심 테스트 #1.
     *
     * X API 호출은 DB Transaction 밖에서 이루어져야 한다.
     *
     * 외부 API 응답을 기다리는 동안
     * DB Connection / Transaction을 잡고 있을 이유가 없기 때문이다.
     */
    @Test
    @DisplayName(
            "X API를 호출하는 동안에는 DB 트랜잭션이 활성화되어 있지 않다."
    )
    void syncPosts_externalApiCall_withoutTransaction() {

        // given
        JsonNode response =
                normalResponse();


        given(
                xClient.callExternalApi(
                        TransferPostSource.FABRIZIO_ROMANO
                )
        )
                .willAnswer(invocation -> {

                    assertThat(
                            TransactionSynchronizationManager
                                    .isActualTransactionActive()
                    )
                            .isFalse();


                    return response;
                });


        // when
        TransferPostsResponseDto result =
                transferPostSyncService.syncPosts(
                        TransferPostSource.FABRIZIO_ROMANO
                );


        // then
        assertThat(result.getPosts())
                .hasSize(2);


        assertThat(transferPostRepository.count())
                .isEqualTo(2);


        then(xClient)
                .should(times(1))
                .callExternalApi(
                        TransferPostSource.FABRIZIO_ROMANO
                );
    }


    // ==================================================
    // 멱등성
    // ==================================================


    @Test
    @DisplayName(
            "동일한 X 게시물을 여러 번 동기화해도 중복 저장하지 않는다."
    )
    void syncPosts_samePosts_idempotent() {

        // given
        JsonNode response =
                normalResponse();


        given(
                xClient.callExternalApi(
                        TransferPostSource.FABRIZIO_ROMANO
                )
        )
                .willReturn(
                        response
                );


        // when
        TransferPostsResponseDto firstResponse =
                transferPostSyncService.syncPosts(
                        TransferPostSource.FABRIZIO_ROMANO
                );


        TransferPostsResponseDto secondResponse =
                transferPostSyncService.syncPosts(
                        TransferPostSource.FABRIZIO_ROMANO
                );


        // then
        assertThat(transferPostRepository.count())
                .isEqualTo(2);


        assertThat(firstResponse.getPosts())
                .hasSize(2);


        assertThat(secondResponse.getPosts())
                .hasSize(2);


        then(xClient)
                .should(times(2))
                .callExternalApi(
                        TransferPostSource.FABRIZIO_ROMANO
                );
    }


    // ==================================================
    // 잘못된 외부 응답
    // ==================================================


    @Test
    @DisplayName(
            "X 응답에 게시물 data가 존재하지 않으면 예외가 발생하고 게시물을 저장하지 않는다."
    )
    void syncPosts_missingData() {

        // given
        JsonNode response =
                readJson(
                        """
                        {
                          "meta": {
                            "result_count": 0
                          }
                        }
                        """
                );


        given(
                xClient.callExternalApi(
                        TransferPostSource.FABRIZIO_ROMANO
                )
        )
                .willReturn(
                        response
                );


        // when & then
        assertThatThrownBy(() ->
                transferPostSyncService.syncPosts(
                        TransferPostSource.FABRIZIO_ROMANO
                )
        )
                .isInstanceOf(
                        InvalidSourcerException.class
                );


        assertThat(transferPostRepository.count())
                .isZero();


        then(xClient)
                .should(times(1))
                .callExternalApi(
                        TransferPostSource.FABRIZIO_ROMANO
                );
    }


    // ==================================================
    // 게시물별 Transaction
    // ==================================================


    /*
     * 이번 리팩토링의 핵심 테스트 #2.
     *
     *
     * Post #1
     *      ↓
     * Tx #1
     *      ↓
     * 정상 저장
     *      ↓
     * COMMIT
     *
     *
     * Post #2
     *      ↓
     * Tx #2
     *      ↓
     * created_at 파싱 실패
     *      ↓
     * ROLLBACK
     *
     *
     * Post #3
     *      ↓
     * Post #2 예외가 SyncService까지 전파되므로
     * 실행되지 않는다.
     *
     *
     * 따라서 최종 DB에는
     * Post #1만 존재해야 한다.
     */
    @Test
    @DisplayName(
            "중간 게시물 처리가 실패하면 이전 게시물은 유지되고 이후 게시물은 처리되지 않는다."
    )
    void syncPosts_secondPostFail_partialCommitAndStop() {

        // given
        JsonNode response =
                secondPostInvalidResponse();


        given(
                xClient.callExternalApi(
                        TransferPostSource.FABRIZIO_ROMANO
                )
        )
                .willReturn(
                        response
                );


        // when & then
        assertThatThrownBy(() ->
                transferPostSyncService.syncPosts(
                        TransferPostSource.FABRIZIO_ROMANO
                )
        )
                .isInstanceOf(
                        DateTimeParseException.class
                );


        /*
         * Post #1
         *
         * 자신의 Transaction에서 정상 저장 후
         * 이미 Commit됐다.
         */
        assertThat(
                transferPostRepository
                        .findByExternalPostId(
                                "1001"
                        )
        )
                .isPresent();


        /*
         * Post #2
         *
         * 처리 도중 실패했다.
         */
        assertThat(
                transferPostRepository
                        .findByExternalPostId(
                                "1002"
                        )
        )
                .isEmpty();


        /*
         * Post #3
         *
         * 두 번째 Post의 예외 때문에
         * 반복문이 중단되어 실행되지 않았다.
         */
        assertThat(
                transferPostRepository
                        .findByExternalPostId(
                                "1003"
                        )
        )
                .isEmpty();


        assertThat(transferPostRepository.count())
                .isEqualTo(1);


        TransferPost firstPost =
                transferPostRepository
                        .findByExternalPostId(
                                "1001"
                        )
                        .orElseThrow();


        assertThat(firstPost.getContent())
                .isEqualTo(
                        "Arsenal have reached an agreement."
                );


        assertThat(firstPost.getContentCreatedAt())
                .isEqualTo(
                        Instant.parse(
                                "2026-10-12T00:00:00Z"
                        )
                );
    }


    // ==================================================
    // Fixture
    // ==================================================


    private JsonNode normalResponse() {

        return readJson(
                """
                {
                  "data": [
                    {
                      "id": "1001",
                      "text": "Arsenal have reached an agreement.",
                      "created_at": "2026-10-12T00:00:00Z"
                    },
                    {
                      "id": "1002",
                      "text": "Chelsea training update.",
                      "created_at": "2026-10-12T00:01:00Z"
                    }
                  ]
                }
                """
        );
    }


    private JsonNode responseWithPhoto() {

        return readJson(
                """
                {
                  "data": [
                    {
                      "id": "1001",
                      "text": "Arsenal transfer update.",
                      "created_at": "2026-10-12T00:00:00Z",
                      "attachments": {
                        "media_keys": [
                          "3_1001"
                        ]
                      }
                    }
                  ],
                  "includes": {
                    "media": [
                      {
                        "media_key": "3_1001",
                        "type": "photo",
                        "url": "https://pbs.twimg.com/media/test-image.jpg",
                        "width": 1200,
                        "height": 800
                      }
                    ]
                  }
                }
                """
        );
    }


    private JsonNode secondPostInvalidResponse() {

        return readJson(
                """
                {
                  "data": [
                    {
                      "id": "1001",
                      "text": "Arsenal have reached an agreement.",
                      "created_at": "2026-10-12T00:00:00Z"
                    },
                    {
                      "id": "1002",
                      "text": "Chelsea transfer update.",
                      "created_at": "invalid-date"
                    },
                    {
                      "id": "1003",
                      "text": "Liverpool transfer update.",
                      "created_at": "2026-10-12T00:02:00Z"
                    }
                  ]
                }
                """
        );
    }


    private JsonNode readJson(
            String json
    ) {

        try {

            return objectMapper
                    .readTree(
                            json
                    );

        } catch (Exception e) {

            throw new IllegalStateException(
                    "테스트 JSON 생성 실패",
                    e
            );
        }
    }
}