package com.dhoon.transfertracker.internal.transferpost.sync.controller;

import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.transferpost.dto.response.TransferPostsResponseDto;
import com.dhoon.transfertracker.internal.transferpost.sync.service.TransferPostSyncService;
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

    private final TransferPostSyncService transferPostSyncService;

    @PostMapping("/external/api/posts/{source}")
    public ResponseEntity<TransferPostsResponseDto> syncPosts(@PathVariable TransferPostSource source) {
        TransferPostsResponseDto responseDto = transferPostSyncService.syncPosts(source);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

}
