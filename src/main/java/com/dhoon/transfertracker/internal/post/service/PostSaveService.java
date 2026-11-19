package com.dhoon.transfertracker.internal.post.service;

import com.dhoon.transfertracker.external.openai.client.OpenAiClient;
import com.dhoon.transfertracker.external.openai.dto.request.TranslationTarget;
import com.dhoon.transfertracker.external.openai.dto.response.TranslationBatchResult;
import com.dhoon.transfertracker.external.openai.dto.response.TranslationResult;
import com.dhoon.transfertracker.external.x.client.XClient;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.post.domain.Post;
import com.dhoon.transfertracker.internal.post.domain.PostJournalist;
import com.dhoon.transfertracker.internal.post.domain.PostPlayer;
import com.dhoon.transfertracker.internal.post.domain.PostTeam;
import com.dhoon.transfertracker.internal.post.event.PostCreatedEvent;
import com.dhoon.transfertracker.internal.post.event.PostTeamAssignedEvent;
import com.dhoon.transfertracker.internal.post.exception.NotFoundPostException;
import com.dhoon.transfertracker.internal.post.record.PostGenerationBatchResult;
import com.dhoon.transfertracker.internal.post.record.PostGenerationBatchTarget;
import com.dhoon.transfertracker.internal.post.record.PostGenerationResult;
import com.dhoon.transfertracker.internal.post.record.PostGenerationTarget;
import com.dhoon.transfertracker.internal.post.repository.PostPlayerRepository;
import com.dhoon.transfertracker.internal.post.repository.PostRepository;
import com.dhoon.transfertracker.internal.post.repository.PostTeamRepository;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.transferpost.command.dto.TeamAssignPostResult;
import com.dhoon.transfertracker.internal.transferpost.command.dto.TeamAssignPostTarget;
import com.dhoon.transfertracker.internal.transferpost.command.dto.TeamAssignPostsResult;
import com.dhoon.transfertracker.internal.transferpost.command.dto.TeamAssignPostsTarget;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class PostSaveService {

    private final XClient xClient;
    private final OpenAiClient openAiClient;

    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;

    private final PostRepository postRepository;
    private final PostTeamRepository postTeamRepository;
    private final PostPlayerRepository postPlayerRepository;
    private final ApplicationEventPublisher eventPublisher;

    public String syncPosts(PostJournalist journalist) {

        JsonNode response = xClient.callExternalApiRefa2(journalist);
        JsonNode userPosts = response.get("data");
        if (userPosts == null || !userPosts.isArray()) {
            throw new IllegalStateException("기자 게시물 응답에 data가 없습니다.");
        }
        List<PostGenerationTarget> generationTargets = new ArrayList<>();
        List<TeamAssignPostTarget> assignPostTargets = new ArrayList<>();
        List<TranslationTarget> translationTargets = new ArrayList<>();
        java.util.Map<String, JsonNode> rawPostByExternalId = new java.util.HashMap<>();

        userPosts.forEach(post -> {
            String externalPostId = post.get("id").asString();
            String content = post.get("text").asString();
            rawPostByExternalId.put(externalPostId, post);

            PostGenerationTarget target = new PostGenerationTarget(externalPostId, journalist.getDisplayerName(), content);

            generationTargets.add(target);
        });


        PostGenerationBatchTarget generationBatchTarget = new PostGenerationBatchTarget(generationTargets);
        PostGenerationBatchResult postGenerationBatchResult = openAiClient.generatePosts(generationBatchTarget);

        List<PostGenerationResult> generatedPosts = postGenerationBatchResult.posts();

        generatedPosts.forEach(generatedPost -> {
            log.info("Post generation result: {}", generatedPost);

            String externalPostId = generatedPost.externalPostId();
            String generationContent = generatedPost.generationContent();
            JsonNode rawPost = rawPostByExternalId.get(externalPostId);
            if (rawPost == null) {
                log.warn("원본 게시물을 찾을 수 없어 저장을 건너뜁니다. externalPostId={}", externalPostId);
                return;
            }
            String createdAt = rawPost.get("created_at").asString();
            Boolean transferRelated = generatedPost.transferRelated();

            Post savedPost = postRepository.findByExternalPostId(externalPostId)
                    .orElseGet(() -> {
                        Post createdPost = postRepository.save(Post.createPost(
                                journalist,
                                generationContent,
                                externalPostId,
                                java.time.Instant.parse(createdAt),
                                Boolean.TRUE.equals(transferRelated)
                        ));
                        eventPublisher.publishEvent(new PostCreatedEvent(createdPost.getId(), journalist));
                        return createdPost;
                    });

            TeamAssignPostTarget assignPostTarget = new TeamAssignPostTarget(savedPost.getId(), savedPost.getContent()); // id -> PK값.
            assignPostTargets.add(assignPostTarget);

            translationTargets.add(new TranslationTarget(savedPost.getId(), savedPost.getContent()));
        });

        // 저장작업 완료 -> 팀 & 플레이어 할당 요청
        TeamAssignPostsResult assignedResults = openAiClient.classifyTeamsAndPlayers(new TeamAssignPostsTarget(assignPostTargets));

        // 포스트에 관련된 팀과 플레이어 할당
        for (TeamAssignPostResult assignResult : assignedResults.posts()) {
            Long postId = assignResult.postId();

            Post post = postRepository.findById(postId)
                    .orElseThrow(NotFoundPostException::new);

            assignResult.teamNames()
                    .forEach(teamName ->
                            teamRepository.findByTeamName(teamName)
                                    .ifPresent(
                                            team -> postTeamRepository.findByTeamIdAndPostId(team.getId(), post.getId())
                                                    .orElseGet(() -> {
                                                        PostTeam postTeam = postTeamRepository.save(PostTeam.postAssignTeam(team, post));
                                                        eventPublisher.publishEvent(new PostTeamAssignedEvent(post.getId(), team.getId()));
                                                        return postTeam;
                                                    }))
                    );

            assignResult.playerNames()
                    .forEach(playerName ->
                            playerRepository.findByPlayerName(playerName)
                                    .ifPresent(
                                            player -> postPlayerRepository.findByPlayerIdAndPostId(player.getId(), post.getId())
                                                    .orElseGet(() -> postPlayerRepository.save(PostPlayer.postAssignPlayer(player, post)))
                                    )
                    );
        }


        // 저장작업 완료 & 팀 - 플레이어 할당 완료 -> 게시물 번역작업 요청
        TranslationBatchResult translatedResults = openAiClient.translate(translationTargets);

        // 포스트 영문내용 한국어로 번역
        for (TranslationResult translationResult : translatedResults.getPosts()) {
            Long postId = translationResult.getPostId();
            String translatedContent = translationResult.getTranslatedContent();

            postRepository.findById(postId)
                    .ifPresent(post -> post.translate(translatedContent));
        }

        return "성공";
    }

    // ========================================================== //

}
