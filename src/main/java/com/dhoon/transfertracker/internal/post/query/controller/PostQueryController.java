package com.dhoon.transfertracker.internal.post.query.controller;

import com.dhoon.transfertracker.internal.post.domain.PostJournalist;
import com.dhoon.transfertracker.internal.post.dto.response.PostItemResponseDto;
import com.dhoon.transfertracker.internal.post.dto.response.PostSourceItemResponseDto;
import com.dhoon.transfertracker.internal.post.dto.response.PostSourcesResponseDto;
import com.dhoon.transfertracker.internal.post.dto.response.PostsResponseDto;
import com.dhoon.transfertracker.internal.post.query.service.PostQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@RestController
@Slf4j
@RequiredArgsConstructor
public class PostQueryController {

    private final PostQueryService postQueryService;

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
    public ResponseEntity<PostItemResponseDto> getPost(@PathVariable Long postId) {
        PostItemResponseDto response = postQueryService.getPost(postId);

        return ResponseEntity.ok(response);
    }

}
