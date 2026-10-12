package com.dhoon.transfertracker.internal.transferpost.controller;

import com.dhoon.transfertracker.external.openai.dto.response.TranslationBatchResult;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.transferpost.dto.response.TransferPostsResponseDto;
import com.dhoon.transfertracker.internal.transferpost.dto.response.TransferPostDetailResponseDto;
import com.dhoon.transfertracker.internal.transferpost.dto.response.TransferPostSourceItemResponseDto;
import com.dhoon.transfertracker.internal.transferpost.dto.response.TransferPostSourcesResponseDto;
import com.dhoon.transfertracker.internal.transferpost.service.impl.TransferPostOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
public class TransferPostController {

    private final TransferPostOrchestrationService transferPostOrchestrationService;

    @GetMapping("/api/transfer/post-sources")
    public ResponseEntity<TransferPostSourcesResponseDto> getPostSources() {
        List<TransferPostSourceItemResponseDto> sources = Arrays.stream(TransferPostSource.values())
                .map(TransferPostSourceItemResponseDto::of)
                .toList();

        return ResponseEntity.ok(TransferPostSourcesResponseDto.of(sources));
    }

    @GetMapping("/api/transfer/post/{postId}")
    public ResponseEntity<TransferPostDetailResponseDto> getTransferPost(@PathVariable Long postId) {
        TransferPostDetailResponseDto response = transferPostOrchestrationService.getTransferPost(postId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/transfer/posts/{sourcer}")
    public ResponseEntity<TransferPostsResponseDto> getSourcerAllPosts(@PathVariable TransferPostSource sourcer) {
        TransferPostsResponseDto response = transferPostOrchestrationService.getSourcerAllPosts(sourcer);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/transfer/posts/team/{teamId}")
    public ResponseEntity<TransferPostsResponseDto> getTeamTransferPosts(@PathVariable Long teamId) {
        TransferPostsResponseDto response = transferPostOrchestrationService.getTeamTransferPosts(teamId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/transfer/posts")
    public ResponseEntity<TransferPostsResponseDto> getAllTransferPosts() {
        TransferPostsResponseDto response = transferPostOrchestrationService.getAllTransferPosts();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/external/api/translate")
    public ResponseEntity<TranslationBatchResult> translate() {
        TranslationBatchResult response = transferPostOrchestrationService.translate();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/external/api/post/assign")
    public ResponseEntity<String> postAssignTeam() {
        String response = transferPostOrchestrationService.postAssignTeam();
        return ResponseEntity.ok(response);
    }


}
