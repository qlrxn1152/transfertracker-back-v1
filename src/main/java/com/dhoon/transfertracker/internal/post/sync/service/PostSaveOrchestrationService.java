package com.dhoon.transfertracker.internal.post.sync.service;

import com.dhoon.transfertracker.external.openai.client.OpenAiClient;
import com.dhoon.transfertracker.external.openai.dto.request.TranslationTarget;
import com.dhoon.transfertracker.external.openai.dto.response.TranslationBatchResult;
import com.dhoon.transfertracker.external.openai.exception.OpenAiApiException;
import com.dhoon.transfertracker.external.x.client.XClient;
import com.dhoon.transfertracker.internal.post.domain.Post;
import com.dhoon.transfertracker.internal.post.domain.PostJournalist;
import com.dhoon.transfertracker.internal.post.domain.PostSyncStatus;
import com.dhoon.transfertracker.internal.post.domain.PostSyncStep;
import com.dhoon.transfertracker.internal.post.notification.PostMemberNotificationService;
import com.dhoon.transfertracker.internal.post.record.*;
import com.dhoon.transfertracker.internal.post.sync.event.PostCreatedEvent;
import com.dhoon.transfertracker.internal.post.sync.event.PostCreatedProducer;
import com.dhoon.transfertracker.internal.post.sync.event.PostEventData;
import com.dhoon.transfertracker.monitoring.PostRetryMetrics;
import com.dhoon.transfertracker.monitoring.PostSyncMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

import static com.dhoon.transfertracker.internal.post.sync.service.PostSaveTxService.*;

@RequiredArgsConstructor
@Slf4j
@Service
public class PostSaveOrchestrationService {

    private final XClient xClient;
    private final OpenAiClient openAiClient;
    private final PostSaveTxService txService;

    private final ObjectProvider<PostCreatedProducer> kafkaProducerProvider;
    private final PostProcessingService postProcessingService;



    public String syncPosts(PostJournalist journalist, Long memberId) {
        JsonNode userPosts = getJournalistPosts(journalist);

        // Open AI 포스트생성 -> 저장
        // 저장작업 완료 -> 팀 & 플레이어 할당 요청
        // 저장작업 완료 & 팀 - 플레이어 할당 완료 -> 게시물 번역작업 요청
        syncPosts(journalist, userPosts);
        return "성공";
    }





    // ===================================================================================== //
    private JsonNode getJournalistPosts(PostJournalist journalist) {
        JsonNode externalResponse = xClient.getJournalistPosts(journalist);
        return externalResponse.get("data");
    }

    private void syncPosts(PostJournalist journalist, JsonNode userPosts) {
        List<PostEventData> posts = new ArrayList<>();

        List<PostGenerationResult> generatedPosts = generateReconstructedPosts(journalist, userPosts).posts(); // 재구성포스트

        if (generatedPosts.isEmpty()) {
            log.info("신규 게시물이 없습니다. journalist = {}", journalist);
            return;
        }

        // 포스트 저장
        generatedPosts.forEach(generatedPost -> {
            Post savedPost = txService.savePost(generatedPost, journalist); // DB

            posts.add(new PostEventData(savedPost.getId(), savedPost.getContent()));
        });

        PostCreatedProducer producer = kafkaProducerProvider.getIfAvailable();

        if (producer != null) {
            producer.send(new PostCreatedEvent(posts));
        } else {
            postProcessingService.process(posts);
        }
    }


    private PostGenerationBatchResult generateReconstructedPosts(PostJournalist journalist, JsonNode userPosts) {
        List<PostGenerationTarget> generationTargets = new ArrayList<>();

        userPosts.forEach(post -> addGenerationTargetIfNew(journalist, post, generationTargets));

        PostGenerationBatchTarget generationBatchTarget = new PostGenerationBatchTarget(generationTargets);
        return openAiClient.generatePosts(generationBatchTarget);
    }

    private void addGenerationTargetIfNew(PostJournalist journalist, JsonNode post, List<PostGenerationTarget> generationTargets) {
        String externalPostId = post.get("id").asString();
        String content = post.get("text").asString();

        boolean isExistPost = txService.checkExistPost(externalPostId);

        if (isExistPost) {
            log.info("이미 존재하는 게시물입니다. external Post ID = {}", externalPostId);
            return;
        }

        generationTargets.add(new PostGenerationTarget(externalPostId, journalist.getDisplayerName(), content));
    }

    public void retryPost(Long postId) {
        postProcessingService.retryPost(postId);
    }


}
