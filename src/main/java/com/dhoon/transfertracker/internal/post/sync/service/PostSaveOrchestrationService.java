package com.dhoon.transfertracker.internal.post.sync.service;

import com.dhoon.transfertracker.external.openai.client.OpenAiClient;
import com.dhoon.transfertracker.external.openai.dto.request.TranslationTarget;
import com.dhoon.transfertracker.external.openai.dto.response.TranslationBatchResult;
import com.dhoon.transfertracker.external.x.client.XClient;
import com.dhoon.transfertracker.internal.post.domain.Post;
import com.dhoon.transfertracker.internal.post.domain.PostJournalist;
import com.dhoon.transfertracker.internal.post.notification.PostMemberNotificationService;
import com.dhoon.transfertracker.internal.post.record.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
public class PostSaveOrchestrationService {

    private final XClient xClient;
    private final OpenAiClient openAiClient;

    private final PostSaveTxService txService;
    private final PostMemberNotificationService postMemberNotificationService;


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
        List<TeamAssignPostTarget> assignPostTargets = new ArrayList<>();
        List<TranslationTarget> translationTargets = new ArrayList<>();
        List<Long> postIds = new ArrayList<>();

        List<PostGenerationResult> generatedPosts = generatePosts(journalist, userPosts).posts();

        if (generatedPosts.isEmpty()) {
            log.info("신규 게시물이 없습니다. journalist = {}", journalist);
            return;
        }

        // 포스트 저장
        generatedPosts.forEach(generatedPost -> {
            Post savedPost = txService.savePost(generatedPost, journalist); // DB

            TeamAssignPostTarget assignPostTarget = new TeamAssignPostTarget(savedPost.getId(), savedPost.getContent());
            assignPostTargets.add(assignPostTarget); // 저장된 게시물을, 할당 대상에 추가.

            TranslationTarget translationTarget = new TranslationTarget(savedPost.getId(), savedPost.getContent());
            translationTargets.add(translationTarget); // 저장된 게시물을, 번역 대상에 추가.

            postIds.add(savedPost.getId());
        });

        assignTeamAndPlayerToPost(assignPostTargets); // 할당
        translatePostsContent(translationTargets); // 번역

        try {
            postMemberNotificationService.sendNotification(postIds);
        } catch (Exception e) {
            log.error("게시물 저장 / 할당 / 번역 작업 완료, 알림 처리중 실패. postIds = {}", postIds, e);
        }
    }







    private PostGenerationBatchResult generatePosts(PostJournalist journalist, JsonNode userPosts) {
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

    private void assignTeamAndPlayerToPost(List<TeamAssignPostTarget> assignPostTargets) {
        TeamAssignPostsResult assignedResults = openAiClient.classifyTeamsAndPlayers(new TeamAssignPostsTarget(assignPostTargets)); // External API

        // 포스트에 관련된 팀과 플레이어 할당
        assignedResults.posts().forEach(txService::assignTeamPlayer);
    }


    private void translatePostsContent(List<TranslationTarget> translationTargets) {
        TranslationBatchResult translatedResults = openAiClient.translate(translationTargets); // External API

        // 포스트 영문내용 한국어로 번역
        translatedResults.getPosts().forEach(txService::translateContent);
    }

}
