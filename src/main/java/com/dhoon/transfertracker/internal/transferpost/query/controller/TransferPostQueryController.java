package com.dhoon.transfertracker.internal.transferpost.query.controller;

import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.transferpost.dto.response.*;
import com.dhoon.transfertracker.internal.transferpost.query.service.impl.TransferPostQueryOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
public class TransferPostQueryController {

    private final TransferPostQueryOrchestrationService queryOrchestrationService;

    @GetMapping("/api/transfer-posts/source/{sourcer}")
    public ResponseEntity<TransferPostsPageResponseDto> getSourcerAllPosts(
            @PathVariable TransferPostSource sourcer,
            @RequestParam(defaultValue = "0") int page) {
        TransferPostsPageResponseDto response = queryOrchestrationService.getSourcerAllPosts(sourcer, page);

        return ResponseEntity.ok(response);
    }














    @GetMapping("/api/transfer-posts/post-sources")
    public ResponseEntity<TransferPostSourcesResponseDto> getPostSources() {
        List<TransferPostSourceItemResponseDto> sources = Arrays.stream(TransferPostSource.values())
                .map(TransferPostSourceItemResponseDto::of)
                .toList();

        return ResponseEntity.ok(TransferPostSourcesResponseDto.of(sources));
    }

    @GetMapping("/api/transfer-posts/{postId}")
    public ResponseEntity<TransferPostDetailResponseDto> getTransferPost(@PathVariable Long postId) {
        TransferPostDetailResponseDto response = queryOrchestrationService.getTransferPost(postId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/transfer-posts/team/{teamId}")
    public ResponseEntity<TransferPostsResponseDto> getTeamTransferPosts(@PathVariable Long teamId) {
        TransferPostsResponseDto response = queryOrchestrationService.getTeamTransferPosts(teamId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/transfer-posts")
    public ResponseEntity<TransferPostsResponseDto> getAllTransferPosts() {
        TransferPostsResponseDto response = queryOrchestrationService.getAllTransferPosts();

        return ResponseEntity.ok(response);
    }
}
