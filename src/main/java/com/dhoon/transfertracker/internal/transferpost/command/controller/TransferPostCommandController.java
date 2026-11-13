package com.dhoon.transfertracker.internal.transferpost.command.controller;

import com.dhoon.transfertracker.internal.transferpost.command.service.impl.TransferPostCommandOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TransferPostCommandController {

    private final TransferPostCommandOrchestrationService transferPostCommandOrchestrationService;

    @PostMapping("/external/api/post/assign")
    public ResponseEntity<String> postAssignTeam() {
        String response = transferPostCommandOrchestrationService.postAssignTeam();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/external/api/posts/notification-test/{teamId}")
    public ResponseEntity<Long> publishNotificationTestPost(@PathVariable Long teamId) {
        Long postId = transferPostCommandOrchestrationService.publishNotificationTestPost(teamId);
        return ResponseEntity.ok(postId);
    }

    /** X API를 호출하지 않고, DB에 이미 저장된 기자 게시물로 팔로워 푸시 흐름만 점검합니다. */
    @PostMapping("/external/api/posts/notification-test/journalist/{postId}")
    public ResponseEntity<Long> publishJournalistNotificationTest(@PathVariable Long postId) {
        Long notifiedPostId = transferPostCommandOrchestrationService.publishJournalistNotificationTest(postId);
        return ResponseEntity.ok(notifiedPostId);
    }

    @DeleteMapping("/external/api/posts/notification-test")
    public ResponseEntity<Integer> removeNotificationTestPosts() {
        return ResponseEntity.ok(transferPostCommandOrchestrationService.removeNotificationTestPosts());
    }




}
