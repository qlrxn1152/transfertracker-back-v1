package com.dhoon.transfertracker.internal.post.query.controller;

import com.dhoon.transfertracker.internal.post.domain.PostJournalist;
import com.dhoon.transfertracker.internal.post.dto.response.PostDetailResponseDto;
import com.dhoon.transfertracker.internal.post.dto.response.PostSourceItemResponseDto;
import com.dhoon.transfertracker.internal.post.dto.response.PostSourcesResponseDto;
import com.dhoon.transfertracker.internal.post.dto.response.PostsResponseDto;
import com.dhoon.transfertracker.internal.post.query.service.PostQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@RestController
@RequiredArgsConstructor
public class PostQueryController {

    private final PostQueryService postQueryService;

    @GetMapping("/api/posts/source/{journalist}")
    public ResponseEntity<PostsResponseDto> getJournalistPosts(
            @PathVariable PostJournalist journalist,
            @RequestParam(defaultValue = "0") int page
    ) {
        return ResponseEntity.ok(postQueryService.getJournalistPosts(journalist, page));
    }

    @GetMapping("/api/posts")
    public ResponseEntity<PostsResponseDto> getPosts(@RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(postQueryService.getPosts(page));
    }

    @GetMapping("/api/posts/team/{teamId}")
    public ResponseEntity<PostsResponseDto> getTeamPosts(
            @PathVariable Long teamId,
            @RequestParam(defaultValue = "0") int page
    ) {
        return ResponseEntity.ok(postQueryService.getTeamPosts(teamId, page));
    }

    @GetMapping("/api/posts/post-sources")
    public ResponseEntity<PostSourcesResponseDto> getPostSources() {
        return ResponseEntity.ok(PostSourcesResponseDto.of(
                Arrays.stream(PostJournalist.values()).map(PostSourceItemResponseDto::of).toList()
        ));
    }

    @GetMapping("/api/posts/{postId}")
    public ResponseEntity<PostDetailResponseDto> getPost(@PathVariable Long postId) {
        return ResponseEntity.ok(postQueryService.getPost(postId));
    }
}
