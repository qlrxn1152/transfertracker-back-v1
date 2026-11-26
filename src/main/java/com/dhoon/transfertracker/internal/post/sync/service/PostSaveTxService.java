package com.dhoon.transfertracker.internal.post.sync.service;

import com.dhoon.transfertracker.external.openai.dto.response.TranslationResult;
import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.post.domain.*;
import com.dhoon.transfertracker.internal.post.exception.NotFoundPostException;
import com.dhoon.transfertracker.internal.post.record.PostGenerationResult;
import com.dhoon.transfertracker.internal.post.record.TeamAssignPostResult;
import com.dhoon.transfertracker.internal.post.repository.PostPlayerRepository;
import com.dhoon.transfertracker.internal.post.repository.PostRepository;
import com.dhoon.transfertracker.internal.post.repository.PostSyncStatusRepository;
import com.dhoon.transfertracker.internal.post.repository.PostTeamRepository;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.team.domain.Team;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class PostSaveTxService {

    private final PostRepository postRepository;
    private final PostSyncStatusRepository postSyncStatusRepository;

    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;

    private final PostTeamRepository postTeamRepository;
    private final PostPlayerRepository postPlayerRepository;


    public Post savePost(PostGenerationResult generatedPost, PostJournalist journalist) {

        String generationContent = generatedPost.generationContent();
        String externalPostId = generatedPost.externalPostId();
        Boolean transferRelated = generatedPost.transferRelated();

        Post savedPost = postRepository.findByExternalPostId(externalPostId)
                .orElseGet(() -> postRepository.save(Post.createPost(
                                journalist,
                                generationContent,
                                externalPostId,
                                transferRelated
                        ))
                );

        PostSyncStatus postSyncStatus = postSyncStatusRepository.findByPostId(savedPost.getId())
                .orElseGet(() -> postSyncStatusRepository.save(PostSyncStatus.createPostSyncStatus(savedPost)));

        return savedPost;
    }

    public void assignTeamPlayer(TeamAssignPostResult assignResult) {
        Long postId = assignResult.postId();

        Post post = postRepository.findById(postId)
                .orElseThrow(NotFoundPostException::new);

        PostSyncStatus postSyncStatus = postSyncStatusRepository.findByPostId(postId).orElseThrow();


        assignResult.teamNames()
                .forEach(teamName ->
                        teamRepository.findByTeamName(teamName)
                                .ifPresent(
                                        team -> postTeamRepository.findByTeamIdAndPostId(team.getId(), post.getId())
                                                .orElseGet(() -> postTeamRepository.save(PostTeam.postAssignTeam(team, post))))
                );

        assignResult.playerNames()
                .forEach(playerName ->
                        playerRepository.findByPlayerName(playerName)
                                .ifPresent(
                                        player -> postPlayerRepository.findByPlayerIdAndPostId(player.getId(), post.getId())
                                                .orElseGet(() -> postPlayerRepository.save(PostPlayer.postAssignPlayer(player, post)))
                                )
                );

        postSyncStatus.complete();
    }

    /**
     * Applies only team/player relationships for legacy posts. It deliberately
     * does not mutate PostSyncStatus, translate content, or send notifications.
     */
    public PostMappingAssignmentCount assignTeamPlayerForBackfill(TeamAssignPostResult assignResult) {
        Post post = postRepository.findById(assignResult.postId())
                .orElseThrow(NotFoundPostException::new);

        int createdTeamMappings = assignResult.teamNames().stream()
                .filter(teamName -> teamName != null && !teamName.isBlank())
                .distinct()
                .map(this::resolveTeam)
                .flatMap(java.util.Optional::stream)
                .mapToInt(team -> postTeamRepository.findByTeamIdAndPostId(team.getId(), post.getId())
                        .map(ignored -> 0)
                        .orElseGet(() -> {
                            postTeamRepository.save(PostTeam.postAssignTeam(team, post));
                            return 1;
                        }))
                .sum();

        int createdPlayerMappings = assignResult.playerNames().stream()
                .filter(playerName -> playerName != null && !playerName.isBlank())
                .distinct()
                .map(this::resolvePlayer)
                .flatMap(java.util.Optional::stream)
                .mapToInt(player -> postPlayerRepository.findByPlayerIdAndPostId(player.getId(), post.getId())
                        .map(ignored -> 0)
                        .orElseGet(() -> {
                            postPlayerRepository.save(PostPlayer.postAssignPlayer(player, post));
                            return 1;
                        }))
                .sum();

        return new PostMappingAssignmentCount(createdTeamMappings, createdPlayerMappings);
    }

    private java.util.Optional<Team> resolveTeam(String teamName) {
        java.util.List<Team> matches = teamRepository.findAllByAnyDisplayName(teamName);
        return matches.size() == 1 ? java.util.Optional.of(matches.getFirst()) : java.util.Optional.empty();
    }

    private java.util.Optional<Player> resolvePlayer(String playerName) {
        java.util.List<Player> matches = playerRepository.findAllByAnyDisplayName(playerName);
        return matches.size() == 1 ? java.util.Optional.of(matches.getFirst()) : java.util.Optional.empty();
    }

    public void translateContent(TranslationResult translationResult) {
        Long postId = translationResult.getPostId();
        String translatedText = translationResult.getTranslatedContent();

        PostSyncStatus postSyncStatus = postSyncStatusRepository.findByPostId(postId).orElseThrow();


        postRepository.findById(postId)
                .ifPresent(post -> post.translate(translatedText));

        postSyncStatus.complete();
    }

    public boolean checkExistPost(String externalPostId) {
        return postRepository.existsByExternalPostId(externalPostId);
    }

    public void startStep(Long postId, PostSyncStep step) {
        PostSyncStatus postSyncStatus = postSyncStatusRepository.findByPostId(postId)
                .orElseThrow();

        if (step != postSyncStatus.getCurrentStep()) {
            throw new IllegalStateException("Step 값이 다릅니다.");
        }

        postSyncStatus.start();
    }

    public void completeStep(Long postId, PostSyncStep step) {
        PostSyncStatus postSyncStatus = postSyncStatusRepository.findByPostId(postId)
                .orElseThrow();

        if (step != postSyncStatus.getCurrentStep()) {
            throw new IllegalStateException("Step 값이 다릅니다.");
        }

        postSyncStatus.complete();
    }

    public void failStep(Long postId, PostSyncStep step, String errorMessage) {
        PostSyncStatus postSyncStatus = postSyncStatusRepository.findByPostId(postId)
                .orElseThrow();

        if (step != postSyncStatus.getCurrentStep()) {
            throw new IllegalStateException("Step 값이 다릅니다.");
        }

        postSyncStatus.fail(errorMessage);
    }

    public void retryStep(Long postId, PostSyncStep step) {
        PostSyncStatus postSyncStatus = postSyncStatusRepository.findByPostId(postId)
                .orElseThrow();

        if ( step != postSyncStatus.getCurrentStep()) {
            throw new IllegalStateException("Step 값이 다릅니다.");
        }

        postSyncStatus.retry();
    }

    public PostRetryData getPostRetryData(Long postId) {
        PostSyncStatus postSyncStatus = postSyncStatusRepository.findByPostId(postId).orElseThrow();

        Post post = postSyncStatus.getPost();

        return new PostRetryData(
                postId,
                post.getContent(),
                postSyncStatus.getCurrentStep()
        );
    }

    public record PostRetryData(Long postId, String content, PostSyncStep step) { }

    public record PostMappingAssignmentCount(int createdTeamMappings, int createdPlayerMappings) { }
}
