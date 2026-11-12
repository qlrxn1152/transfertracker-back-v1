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

    @DeleteMapping("/external/api/posts/notification-test")
    public ResponseEntity<Integer> removeNotificationTestPosts() {
        return ResponseEntity.ok(transferPostCommandOrchestrationService.removeNotificationTestPosts());
    }




}
