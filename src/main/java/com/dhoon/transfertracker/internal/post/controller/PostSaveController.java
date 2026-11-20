package com.dhoon.transfertracker.internal.post.controller;

import com.dhoon.transfertracker.internal.post.domain.PostJournalist;
import com.dhoon.transfertracker.internal.post.dto.response.PostDetailResponseDto;
import com.dhoon.transfertracker.internal.post.dto.response.PostSourceItemResponseDto;
import com.dhoon.transfertracker.internal.post.dto.response.PostSourcesResponseDto;
import com.dhoon.transfertracker.internal.post.dto.response.PostsResponseDto;
import com.dhoon.transfertracker.internal.post.service.PostQueryService;
import com.dhoon.transfertracker.internal.post.service.PostSaveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
@Slf4j
@RequiredArgsConstructor
public class PostSaveController {

    private final PostSaveService postSaveService;
    private final PostQueryService postQueryService;

    @PostMapping("/external/api/posts/{journalist}")
    public ResponseEntity<String> savePosts(@PathVariable PostJournalist journalist, @AuthenticationPrincipal Jwt jwt) {
        Long memberId = Long.valueOf(jwt.getSubject());
        String response = postSaveService.syncPosts(journalist, memberId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @GetMapping("/api/posts")
    public ResponseEntity<PostsResponseDto> getPosts(@RequestParam(defaultValue = "0") int page) {
        PostsResponseDto response = postQueryService.getPosts(page);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/posts/sources")
    public ResponseEntity<PostSourcesResponseDto> getPostSources() {
        PostSourcesResponseDto response = PostSourcesResponseDto.of(
                Arrays.stream(PostJournalist.values())
                        .map(PostSourceItemResponseDto::of)
                        .toList()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/posts/journalists/{journalist}")
    public ResponseEntity<PostsResponseDto> getJournalistPosts(@PathVariable PostJournalist journalist, @RequestParam(defaultValue = "0") int page) {
        PostsResponseDto response = postQueryService.getJournalistPosts(journalist, page);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/posts/teams/{teamId}")
    public ResponseEntity<PostsResponseDto> getTeamPosts(@PathVariable Long teamId, @RequestParam(defaultValue = "0") int page) {
        PostsResponseDto response = postQueryService.getTeamPosts(teamId, page);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/posts/players/{playerId}")
    public ResponseEntity<PostsResponseDto> getPlayerPosts(@PathVariable Long playerId, @RequestParam(defaultValue = "0") int page) {
        PostsResponseDto response = postQueryService.getPlayerPosts(playerId, page);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/posts/{postId}")
    public ResponseEntity<PostDetailResponseDto> getPost(@PathVariable Long postId) {
        PostDetailResponseDto response = postQueryService.getPost(postId);

        return ResponseEntity.ok(response);
    }

}
