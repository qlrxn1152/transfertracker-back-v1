package com.dhoon.transfertracker.internal.post.sync.service;

import com.dhoon.transfertracker.external.openai.client.OpenAiClient;
import com.dhoon.transfertracker.external.openai.dto.request.TranslationTarget;
import com.dhoon.transfertracker.external.openai.dto.response.TranslationBatchResult;
import com.dhoon.transfertracker.external.openai.exception.OpenAiApiException;
import com.dhoon.transfertracker.internal.post.domain.PostSyncStep;
import com.dhoon.transfertracker.internal.post.notification.PostMemberNotificationService;
import com.dhoon.transfertracker.internal.post.record.TeamAssignPostTarget;
import com.dhoon.transfertracker.internal.post.record.TeamAssignPostsResult;
import com.dhoon.transfertracker.internal.post.record.TeamAssignPostsTarget;
import com.dhoon.transfertracker.internal.post.sync.event.PostEventData;
import com.dhoon.transfertracker.monitoring.PostRetryMetrics;
import com.dhoon.transfertracker.monitoring.PostSyncMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostProcessingService {

    private final PostSaveTxService txService;
    private final PostSyncMetrics postSyncMetrics;
    private final PostRetryMetrics postRetryMetrics;
    private final OpenAiClient openAiClient;
    private final PostMemberNotificationService postMemberNotificationService;

    public void process(List<PostEventData> posts) {
        List<TeamAssignPostTarget> assignPostTargets = new ArrayList<>();
        List<TranslationTarget> translationTargets = new ArrayList<>();
        List<Long> postIds = new ArrayList<>();

        posts.forEach(post -> {
                    Long postId = post.postId();
                    String content = post.content();

                    assignPostTargets.add(new TeamAssignPostTarget(postId, content));
                    translationTargets.add(new TranslationTarget(postId, content));
                    postIds.add(postId);
                });


        assignTeamAndPlayerToPost(assignPostTargets);
        translatePostsContent(translationTargets);
        sendNotification(postIds);
    }


    private void assignTeamAndPlayerToPost(List<TeamAssignPostTarget> assignPostTargets) {
        assignPostTargets.forEach(target -> {
            Long postId = target.postId();
            txService.startStep(postId, PostSyncStep.ASSIGN);
        });

        executeAssign(assignPostTargets);
    }

    private void executeAssign(List<TeamAssignPostTarget> assignPostTargets) {
        TeamAssignPostsResult assignedResults;

        try{
            assignedResults = openAiClient.classifyTeamsAndPlayers(new TeamAssignPostsTarget(assignPostTargets));
        } catch (OpenAiApiException e) {
            assignPostTargets.forEach(target -> {
                Long postId = target.postId();
                txService.failStep(postId, PostSyncStep.ASSIGN, e.getMessage());
                postSyncMetrics.failure(PostSyncStep.ASSIGN);
            });
            throw e;
        }

        // 포스트에 관련된 팀과 플레이어 할당
        assignedResults.posts().forEach(result -> {
            txService.assignTeamPlayer(result);
            // Tx service 호출이 정상 반환된 뒤에만 성공을 기록한다. (트랜잭션 커밋 완료 이후)
            postSyncMetrics.success(PostSyncStep.ASSIGN);
        });
    }



    private void translatePostsContent(List<TranslationTarget> translationTargets) {
        translationTargets.forEach(target -> {
            Long postId = target.getPostId();
            txService.startStep(postId, PostSyncStep.TRANSLATE);
        });

        executeTranslate(translationTargets);
    }

    private void executeTranslate(List<TranslationTarget> translationTargets) {
        TranslationBatchResult translatedResults;

        try {
            translatedResults = openAiClient.translate(translationTargets);
        } catch (OpenAiApiException e) {
            translationTargets.forEach(target -> {
                Long postId = target.getPostId();
                txService.failStep(postId, PostSyncStep.TRANSLATE, e.getMessage());
                postSyncMetrics.failure(PostSyncStep.TRANSLATE);
            });
            throw e;
        }

        // 포스트 영문내용 한국어로 번역
        translatedResults.getPosts().forEach(result -> {
            txService.translateContent(result);
            postSyncMetrics.success(PostSyncStep.TRANSLATE);
        });
    }




    private void sendNotification(List<Long> postIds) {
        postIds.forEach(postId -> txService.startStep(postId, PostSyncStep.NOTIFICATION));

        executeNotification(postIds);
    }

    private void executeNotification(List<Long> postIds) {
        try {
            postMemberNotificationService.sendNotification(postIds);

        } catch (Exception e) {
            postIds.forEach(postId -> {
                txService.failStep(postId, PostSyncStep.NOTIFICATION, "Notification Send Failed");
                postSyncMetrics.failure(PostSyncStep.NOTIFICATION);
            });

            log.error("게시물 저장 / 할당 / 번역 작업 완료, 알림 처리중 실패. postIds = {}", postIds, e);
            throw e;
        }
        postIds.forEach(postId -> {
            txService.completeStep(postId, PostSyncStep.NOTIFICATION);
            postSyncMetrics.success(PostSyncStep.NOTIFICATION);
        });
    }

    public void retryPost(Long postId) {
        PostSaveTxService.PostRetryData retryData = txService.getPostRetryData(postId);
        PostSyncStep failedStep = retryData.step();

        TeamAssignPostTarget assignTarget = new TeamAssignPostTarget(
                retryData.postId(),
                retryData.content()
        );

        TranslationTarget translationTarget = new TranslationTarget(
                retryData.postId(),
                retryData.content()
        );

        txService.retryStep(postId, failedStep);
        postRetryMetrics.accepted(failedStep);

        try {
            switch (failedStep) {
                case ASSIGN -> {
                    executeAssign(List.of(assignTarget));
                    translatePostsContent(List.of(translationTarget));
                    sendNotification(List.of(postId));
                }
                case TRANSLATE -> {
                    executeTranslate(List.of(translationTarget));
                    sendNotification(List.of(postId));
                }
                case NOTIFICATION -> executeNotification(List.of(postId));
            }
            postRetryMetrics.completed(failedStep);
        } catch (RuntimeException exception) {
            postRetryMetrics.failed(failedStep);
            throw exception;
        }

    }


}
