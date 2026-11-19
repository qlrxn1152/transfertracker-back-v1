package com.dhoon.transfertracker.internal.transferpost.sync.controller;

import com.dhoon.transfertracker.internal.post.domain.PostJournalist;
import com.dhoon.transfertracker.internal.post.service.PostSaveService;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.transferpost.dto.response.TransferPostsResponseDto;
import com.dhoon.transfertracker.internal.transferpost.refa.TransferPostRefa;
import com.dhoon.transfertracker.internal.transferpost.sync.dto.response.TransferPostSyncResponseDto;
import com.dhoon.transfertracker.internal.transferpost.sync.service.impl.TransferPostSyncOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class TransferPostSyncController {

    private final TransferPostSyncOrchestrationService transferPostSyncService;

    private final TransferPostRefa transferPostRefa;

    private final PostSaveService postSaveService;

    @PostMapping("/external/api/posts/{journalist}")
    public ResponseEntity<String> syncPosts(@PathVariable PostJournalist journalist) {
        String response = postSaveService.syncPosts(journalist);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}
