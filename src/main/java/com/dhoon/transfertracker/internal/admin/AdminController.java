package com.dhoon.transfertracker.internal.admin;

import com.dhoon.transfertracker.internal.post.sync.service.PostSaveOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class AdminController {

    private final PostSaveOrchestrationService orchService;

    @PostMapping("/api/admin/posts/{postId}/retry")
    public ResponseEntity<Void> retryPost(@PathVariable Long postId) {
        orchService.retryPost(postId);

        return ResponseEntity.ok().build();
    }
}
