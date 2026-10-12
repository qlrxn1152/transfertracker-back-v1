package com.dhoon.transfertracker.etc.guide.controller;

import com.dhoon.transfertracker.etc.guide.dto.request.GuidePostCreateRequestDto;
import com.dhoon.transfertracker.etc.guide.dto.response.GuidePostItemResponseDto;
import com.dhoon.transfertracker.etc.guide.dto.response.GuidePostsResponseDto;
import com.dhoon.transfertracker.etc.guide.service.GuidePostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GuidePostController {

    private final GuidePostService guidePostService;

    @GetMapping("/api/guide-posts")
    public ResponseEntity<GuidePostsResponseDto> getGuidePosts() {
        return ResponseEntity.ok(guidePostService.getGuidePosts());
    }

    @GetMapping("/api/guide-posts/{guidePostId}")
    public ResponseEntity<GuidePostItemResponseDto> getGuidePost(@PathVariable Long guidePostId) {
        return guidePostService.findGuidePost(guidePostId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/api/guide-post-detail/{guidePostId}")
    public ResponseEntity<GuidePostItemResponseDto> getGuidePostDetail(@PathVariable Long guidePostId) {
        return guidePostService.findGuidePost(guidePostId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/api/guide-posts")
    public ResponseEntity<GuidePostItemResponseDto> createGuidePost(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody GuidePostCreateRequestDto request
    ) {
        GuidePostItemResponseDto response = guidePostService.createGuidePost(Long.valueOf(jwt.getSubject()), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/api/guide-posts/{guidePostId}")
    public ResponseEntity<GuidePostItemResponseDto> updateGuidePost(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long guidePostId,
            @Valid @RequestBody GuidePostCreateRequestDto request
    ) {
        GuidePostItemResponseDto response = guidePostService.updateGuidePost(Long.valueOf(jwt.getSubject()), guidePostId, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/api/guide-posts/{guidePostId}")
    public ResponseEntity<Void> deleteGuidePost(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long guidePostId
    ) {
        guidePostService.deleteGuidePost(Long.valueOf(jwt.getSubject()), guidePostId);

        return ResponseEntity.noContent().build();
    }
}
