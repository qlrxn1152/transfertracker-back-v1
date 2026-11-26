package com.dhoon.transfertracker.internal.post.sync.service;

import com.dhoon.transfertracker.external.openai.client.OpenAiClient;
import com.dhoon.transfertracker.internal.post.domain.Post;
import com.dhoon.transfertracker.internal.post.record.TeamAssignPostResult;
import com.dhoon.transfertracker.internal.post.record.TeamAssignPostsResult;
import com.dhoon.transfertracker.internal.post.repository.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class PostMappingBackfillServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostSaveTxService postSaveTxService;

    @Mock
    private OpenAiClient openAiClient;

    @InjectMocks
    private PostMappingBackfillService postMappingBackfillService;

    @Test
    @DisplayName("미연결 레거시 게시물은 매핑만 백필하고 다음 커서를 반환한다")
    void backfill_success() {
        Post post = org.mockito.Mockito.mock(Post.class);
        given(post.getId()).willReturn(101L);
        given(post.getContent()).willReturn("Real Madrid are discussing a new deal with Jude Bellingham.");
        given(postRepository.findMappingBackfillTargets(eq(0L), any(Pageable.class)))
                .willReturn(List.of(post));
        given(openAiClient.classifyTeamsAndPlayers(any()))
                .willReturn(new TeamAssignPostsResult(List.of(
                        new TeamAssignPostResult(101L, List.of("Real Madrid"), List.of("Jude Bellingham"))
                )));
        given(postSaveTxService.assignTeamPlayerForBackfill(any()))
                .willReturn(new PostSaveTxService.PostMappingAssignmentCount(1, 1));

        PostMappingBackfillService.PostMappingBackfillResult result =
                postMappingBackfillService.backfill(0L, 25);

        assertThat(result.targetedPosts()).isEqualTo(1);
        assertThat(result.mappedPosts()).isEqualTo(1);
        assertThat(result.createdTeamMappings()).isEqualTo(1);
        assertThat(result.createdPlayerMappings()).isEqualTo(1);
        assertThat(result.nextAfterPostId()).isEqualTo(101L);
        assertThat(result.completed()).isTrue();

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        then(postRepository).should().findMappingBackfillTargets(eq(0L), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(26);
        then(postSaveTxService).should().assignTeamPlayerForBackfill(
                new TeamAssignPostResult(101L, List.of("Real Madrid"), List.of("Jude Bellingham"))
        );
    }
}
