package com.dhoon.transfertracker.internal.post.sync.service;

import com.dhoon.transfertracker.external.openai.client.OpenAiClient;
import com.dhoon.transfertracker.internal.post.domain.Post;
import com.dhoon.transfertracker.internal.post.record.TeamAssignPostResult;
import com.dhoon.transfertracker.internal.post.record.TeamAssignPostTarget;
import com.dhoon.transfertracker.internal.post.record.TeamAssignPostsResult;
import com.dhoon.transfertracker.internal.post.record.TeamAssignPostsTarget;
import com.dhoon.transfertracker.internal.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mapping-only recovery for posts saved before the current post-processing
 * pipeline existed. Translation, sync status, and FCM delivery are excluded.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PostMappingBackfillService {

    private static final int DEFAULT_BATCH_SIZE = 25;
    private static final int MAX_BATCH_SIZE = 25;

    private final PostRepository postRepository;
    private final PostSaveTxService postSaveTxService;
    private final OpenAiClient openAiClient;

    public PostMappingBackfillResult backfill(Long afterPostId, Integer requestedBatchSize) {
        long cursor = afterPostId == null ? 0L : Math.max(afterPostId, 0L);
        int batchSize = requestedBatchSize == null
                ? DEFAULT_BATCH_SIZE
                : Math.clamp(requestedBatchSize, 1, MAX_BATCH_SIZE);

        List<Post> candidates = postRepository.findMappingBackfillTargets(
                cursor,
                PageRequest.of(0, batchSize + 1)
        );
        if (candidates.isEmpty()) {
            return PostMappingBackfillResult.empty(cursor);
        }

        boolean completed = candidates.size() <= batchSize;
        List<Post> posts = completed ? candidates : candidates.subList(0, batchSize);
        List<TeamAssignPostTarget> targets = posts.stream()
                .map(post -> new TeamAssignPostTarget(post.getId(), post.getContent()))
                .toList();

        TeamAssignPostsResult assignments = openAiClient.classifyTeamsAndPlayers(new TeamAssignPostsTarget(targets));
        Map<Long, TeamAssignPostResult> assignmentByPostId = validateAndIndexResults(posts, assignments);

        int createdTeamMappings = 0;
        int createdPlayerMappings = 0;
        int mappedPosts = 0;
        for (Post post : posts) {
            PostSaveTxService.PostMappingAssignmentCount count =
                    postSaveTxService.assignTeamPlayerForBackfill(assignmentByPostId.get(post.getId()));
            createdTeamMappings += count.createdTeamMappings();
            createdPlayerMappings += count.createdPlayerMappings();
            if (count.createdTeamMappings() + count.createdPlayerMappings() > 0) {
                mappedPosts++;
            }
        }

        long nextAfterPostId = posts.getLast().getId();
        log.info(
                "Legacy post mapping backfill complete. targets={}, mappedPosts={}, teamMappings={}, playerMappings={}, nextAfterPostId={}",
                posts.size(), mappedPosts, createdTeamMappings, createdPlayerMappings, nextAfterPostId
        );

        return new PostMappingBackfillResult(
                posts.size(),
                mappedPosts,
                createdTeamMappings,
                createdPlayerMappings,
                nextAfterPostId,
                completed
        );
    }

    private Map<Long, TeamAssignPostResult> validateAndIndexResults(
            List<Post> posts,
            TeamAssignPostsResult assignments
    ) {
        if (assignments == null || assignments.posts() == null) {
            throw new IllegalStateException("게시물 매핑 결과가 없습니다.");
        }

        Set<Long> targetPostIds = posts.stream().map(Post::getId).collect(Collectors.toSet());
        Map<Long, TeamAssignPostResult> results = new HashMap<>();
        for (TeamAssignPostResult result : assignments.posts()) {
            if (result == null || result.postId() == null || !targetPostIds.contains(result.postId())
                    || results.put(result.postId(), result) != null) {
                throw new IllegalStateException("게시물 매핑 결과의 postId가 올바르지 않습니다.");
            }
            if (result.teamNames() == null || result.playerNames() == null) {
                throw new IllegalStateException("게시물 매핑 결과에 팀 또는 선수 목록이 없습니다.");
            }
        }

        if (!results.keySet().equals(targetPostIds)) {
            Set<Long> missingPostIds = new HashSet<>(targetPostIds);
            missingPostIds.removeAll(results.keySet());
            throw new IllegalStateException("게시물 매핑 결과가 일부 누락되었습니다. missingPostIds=" + missingPostIds);
        }
        return results;
    }

    public record PostMappingBackfillResult(
            int targetedPosts,
            int mappedPosts,
            int createdTeamMappings,
            int createdPlayerMappings,
            long nextAfterPostId,
            boolean completed
    ) {
        private static PostMappingBackfillResult empty(long cursor) {
            return new PostMappingBackfillResult(0, 0, 0, 0, cursor, true);
        }
    }
}
