package com.dhoon.transfertracker.internal.controller;

import com.dhoon.transfertracker.internal.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.dto.response.TransferPostsResponseDto;
import com.dhoon.transfertracker.internal.service.TransferPostService;
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

}
