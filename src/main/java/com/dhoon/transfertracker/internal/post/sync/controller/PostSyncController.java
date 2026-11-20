package com.dhoon.transfertracker.internal.post.sync.controller;

import com.dhoon.transfertracker.internal.post.domain.PostJournalist;
import com.dhoon.transfertracker.internal.post.sync.service.PostSaveOrchestrationService;
import com.dhoon.transfertracker.internal.post.sync.service.PostSaveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class PostSyncController {

    private final PostSaveOrchestrationService postOrchService;

    @PostMapping("/external/api/posts/{journalist}")
    public ResponseEntity<String> savePosts(@PathVariable PostJournalist journalist, @AuthenticationPrincipal Jwt jwt) {
        Long memberId = Long.valueOf(jwt.getSubject());
        String response = postOrchService.syncPosts(journalist, memberId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}
