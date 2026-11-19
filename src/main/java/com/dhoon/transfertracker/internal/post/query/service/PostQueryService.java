package com.dhoon.transfertracker.internal.post.query.service;

import com.dhoon.transfertracker.internal.post.domain.Post;
import com.dhoon.transfertracker.internal.post.domain.PostJournalist;
import com.dhoon.transfertracker.internal.post.domain.PostTeam;
import com.dhoon.transfertracker.internal.post.dto.response.PostDetailResponseDto;
import com.dhoon.transfertracker.internal.post.dto.response.PostItemResponseDto;
import com.dhoon.transfertracker.internal.post.dto.response.PostsResponseDto;
import com.dhoon.transfertracker.internal.post.exception.InvalidPostIdException;
import com.dhoon.transfertracker.internal.post.exception.NotFoundPostException;
import com.dhoon.transfertracker.internal.post.repository.PostRepository;
import com.dhoon.transfertracker.internal.post.repository.PostTeamRepository;
import com.dhoon.transfertracker.internal.team.exception.NotFoundTeamException;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostQueryService {

    private static final int PAGE_SIZE = 20;

    private final PostRepository postRepository;
    private final PostTeamRepository postTeamRepository;
    private final TeamRepository teamRepository;

    public PostsResponseDto getJournalistPosts(PostJournalist journalist, int page) {
        Page<Post> posts = postRepository.findAllByJournalist(journalist, postPageable(page));
        return toResponse(posts);
    }

    public PostsResponseDto getPosts(int page) {
        Page<Post> posts = postRepository.findAll(postPageable(page));
        return toResponse(posts);
    }

    public PostsResponseDto getTeamPosts(Long teamId, int page) {
        teamRepository.findById(teamId).orElseThrow(NotFoundTeamException::new);
        Page<PostTeam> posts = postTeamRepository.findAllByTeamId(teamId, teamPageable(page));
        return PostsResponseDto.of(
                posts.getContent().stream().map(PostItemResponseDto::of).toList(),
                posts.getTotalElements(),
                posts.getTotalPages(),
                posts.hasNext(),
                posts.hasPrevious()
        );
    }

    public PostDetailResponseDto getPost(Long postId) {
        if (postId == null) {
            throw new InvalidPostIdException();
        }
        Post post = postRepository.findById(postId).orElseThrow(NotFoundPostException::new);
        return PostDetailResponseDto.of(PostItemResponseDto.of(post));
    }

    private Pageable postPageable(int page) {
        validatePage(page);
        return PageRequest.of(page, PAGE_SIZE, Sort.by(
                Sort.Order.desc("contentCreatedAt"),
                Sort.Order.desc("id")
        ));
    }

    private Pageable teamPageable(int page) {
        validatePage(page);
        return PageRequest.of(page, PAGE_SIZE, Sort.by(
                Sort.Order.desc("post.contentCreatedAt"),
                Sort.Order.desc("id")
        ));
    }

    private PostsResponseDto toResponse(Page<Post> posts) {
        return PostsResponseDto.of(
                posts.getContent().stream().map(PostItemResponseDto::of).toList(),
                posts.getTotalElements(),
                posts.getTotalPages(),
                posts.hasNext(),
                posts.hasPrevious()
        );
    }

    private void validatePage(int page) {
        if (page < 0) {
            throw new IllegalArgumentException("페이지 값은 0보다 작을 수 없습니다.");
        }
    }
}
