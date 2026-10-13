package com.dhoon.transfertracker.internal.transferpost.query.controller;

import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.transferpost.dto.response.TransferPostDetailResponseDto;
import com.dhoon.transfertracker.internal.transferpost.dto.response.TransferPostSourceItemResponseDto;
import com.dhoon.transfertracker.internal.transferpost.dto.response.TransferPostSourcesResponseDto;
import com.dhoon.transfertracker.internal.transferpost.dto.response.TransferPostsResponseDto;
import com.dhoon.transfertracker.internal.transferpost.query.service.impl.TransferPostQueryOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
public class TransferPostQueryController {

    private final TransferPostQueryOrchestrationService queryOrchestrationService;

    @GetMapping("/api/transfer/post-sources")
    public ResponseEntity<TransferPostSourcesResponseDto> getPostSources() {
        List<TransferPostSourceItemResponseDto> sources = Arrays.stream(TransferPostSource.values())
                .map(TransferPostSourceItemResponseDto::of)
                .toList();

        return ResponseEntity.ok(TransferPostSourcesResponseDto.of(sources));
    }

    @GetMapping("/api/transfer/post/{postId}")
    public ResponseEntity<TransferPostDetailResponseDto> getTransferPost(@PathVariable Long postId) {
        TransferPostDetailResponseDto response = queryOrchestrationService.getTransferPost(postId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/transfer/posts/{sourcer}")
    public ResponseEntity<TransferPostsResponseDto> getSourcerAllPosts(@PathVariable TransferPostSource sourcer) {
        TransferPostsResponseDto response = queryOrchestrationService.getSourcerAllPosts(sourcer);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/transfer/posts/team/{teamId}")
    public ResponseEntity<TransferPostsResponseDto> getTeamTransferPosts(@PathVariable Long teamId) {
        TransferPostsResponseDto response = queryOrchestrationService.getTeamTransferPosts(teamId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/transfer/posts")
    public ResponseEntity<TransferPostsResponseDto> getAllTransferPosts() {
        TransferPostsResponseDto response = queryOrchestrationService.getAllTransferPosts();

        return ResponseEntity.ok(response);
    }
}
