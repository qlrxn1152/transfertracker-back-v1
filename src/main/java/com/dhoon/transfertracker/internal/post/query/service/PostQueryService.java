package com.dhoon.transfertracker.internal.post.query.service;

import com.dhoon.transfertracker.internal.player.exception.InvalidPlayerIdException;
import com.dhoon.transfertracker.internal.player.exception.NotFoundPlayerException;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.post.domain.Post;
import com.dhoon.transfertracker.internal.post.domain.PostJournalist;
import com.dhoon.transfertracker.internal.post.domain.PostPlayer;
import com.dhoon.transfertracker.internal.post.domain.PostTeam;
import com.dhoon.transfertracker.internal.post.dto.response.PostItemResponseDto;
import com.dhoon.transfertracker.internal.post.dto.response.PostsResponseDto;
import com.dhoon.transfertracker.internal.post.exception.InvalidPageException;
import com.dhoon.transfertracker.internal.post.exception.InvalidPostIdException;
import com.dhoon.transfertracker.internal.post.exception.NotFoundPostException;
import com.dhoon.transfertracker.internal.post.repository.PostPlayerRepository;
import com.dhoon.transfertracker.internal.post.repository.PostRepository;
import com.dhoon.transfertracker.internal.post.repository.PostTeamRepository;
import com.dhoon.transfertracker.internal.team.exception.InvalidTeamIdException;
import com.dhoon.transfertracker.internal.team.exception.NotFoundTeamException;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.teamplayer.domain.TeamPlayer;
import com.dhoon.transfertracker.internal.teamplayer.repository.TeamPlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostQueryService {

    private final PostRepository postRepository;
    private final PostTeamRepository postTeamRepository;
    private final PostPlayerRepository postPlayerRepository;

    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final TeamPlayerRepository teamPlayerRepository;

    public PostsResponseDto getPosts(int page) {
        validatePageNotNegative(page);
        Pageable pageable = createPostPage(page);

        Page<Post> postPage = postRepository.findAll(pageable);

        List<PostItemResponseDto> posts = postPage.getContent()
                .stream()
                .map(PostItemResponseDto::createPost)
                .toList();

        return PostsResponseDto.of(posts, postPage.getTotalElements(), postPage.getTotalPages(), postPage.hasNext(), postPage.hasPrevious());
    }

    public PostsResponseDto getJournalistPosts(PostJournalist journalist, int page) {
        if (journalist == null) {
            throw new IllegalArgumentException("Journalist 는 null 일 수 없습니다.");
        }

        validatePageNotNegative(page);
        Pageable pageable = createPostPage(page);

        Page<Post> postPage = postRepository.findAllByJournalist(journalist, pageable);

        List<PostItemResponseDto> posts = postPage.getContent()
                .stream()
                .map(PostItemResponseDto::createPost)
                .toList();

        return PostsResponseDto.of(posts, postPage.getTotalElements(), postPage.getTotalPages(), postPage.hasNext(), postPage.hasPrevious());
    }

    public PostItemResponseDto getPost(Long postId) {
        if (postId == null || postId <= 0) {
            throw new InvalidPostIdException();
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(NotFoundPostException::new);

        return PostItemResponseDto.createPost(post);
    }

    public PostsResponseDto getTeamPosts(Long teamId, int page) {
        if (teamId == null || teamId <= 0) {
            throw new InvalidTeamIdException();
        }
        validatePageNotNegative(page);

        teamRepository.findById(teamId)
                .orElseThrow(NotFoundTeamException::new);

        Pageable pageable = createPostPage(page);

        Page<PostTeam> postPage = postTeamRepository.findAllByTeamId(teamId, pageable);

        List<PostItemResponseDto> posts = postPage.getContent()
                .stream()
                .map(PostItemResponseDto::createTeamPost)
                .toList();

        return PostsResponseDto.of(posts, postPage.getTotalElements(), postPage.getTotalPages(), postPage.hasNext(), postPage.hasPrevious());
    }

    public PostsResponseDto getPlayerPosts(Long playerId, int page) {
        if (playerId == null || playerId <= 0) {
            throw new InvalidPlayerIdException();
        }

        validatePageNotNegative(page);

        playerRepository.findById(playerId)
                .orElseThrow(NotFoundPlayerException::new);

        Pageable pageable = createPostPage(page);

        Page<PostPlayer> postPage = postPlayerRepository.findAllByPlayerId(playerId, pageable);

        if (!teamPlayerRepository.existsByPlayerId(playerId)) {
            List<PostItemResponseDto> posts = postPage.getContent()
                    .stream()
                    .map(PostItemResponseDto::createFaPlayer)
                    .toList();

            return PostsResponseDto.of(posts, postPage.getTotalElements(), postPage.getTotalPages(), postPage.hasNext(), postPage.hasPrevious());
        }

        TeamPlayer teamPlayer = teamPlayerRepository.findByPlayerId(playerId)
                .orElseThrow(NotFoundPlayerException::new);

        List<PostItemResponseDto> posts = postPage.getContent()
                .stream()
                .map(postPlayer -> PostItemResponseDto.createTeamPlayer(postPlayer, teamPlayer))
                .toList();

        return PostsResponseDto.of(posts, postPage.getTotalElements(), postPage.getTotalPages(), postPage.hasNext(), postPage.hasPrevious());
    }

    // ======================================================================================================== //
    private Pageable createPostPage(int page) {
        return PageRequest.of(
                page,
                30,
                Sort.by(Sort.Order.desc("contentCreatedAt"), Sort.Order.desc("id"))
        );
    }

    private void validatePageNotNegative(int page) {
        if (page < 0) {
            throw new InvalidPageException();
        }
    }


}
