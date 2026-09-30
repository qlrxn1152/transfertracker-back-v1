package com.dhoon.transfertracker.internal.transferpost.controller;

import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.transfer.dto.response.TransferPostsResponseDto;
import com.dhoon.transfertracker.internal.transfer.service.TransferPostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class TransferPostController {

    private final TransferPostService transferPostService;

    @GetMapping("/api/transfer/posts/{sourcer}")
    public ResponseEntity<TransferPostsResponseDto> getSourcerAllPosts(@PathVariable TransferPostSource sourcer) {
        TransferPostsResponseDto response = transferPostService.getSourcerAllPosts(sourcer);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/transfer/posts/team/{teamId}")
    public ResponseEntity<TransferPostsResponseDto> getTeamTransferPosts(@PathVariable Long teamId) {
        TransferPostsResponseDto response = transferPostService.getTeamTransferPosts(teamId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/transfer/posts")
    public ResponseEntity<TransferPostsResponseDto> getAllTransferPosts() {
        TransferPostsResponseDto response = transferPostService.getAllTransferPosts();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/test")
    public ResponseEntity<String> test() {
        String response = transferPostService.test();
        return ResponseEntity.ok(response);
    }



}
