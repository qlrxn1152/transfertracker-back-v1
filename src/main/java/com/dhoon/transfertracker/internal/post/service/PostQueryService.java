package com.dhoon.transfertracker.internal.post.service;

import com.dhoon.transfertracker.internal.player.exception.InvalidPlayerIdException;
import com.dhoon.transfertracker.internal.player.exception.NotFoundPlayerException;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.post.domain.Post;
import com.dhoon.transfertracker.internal.post.domain.PostJournalist;
import com.dhoon.transfertracker.internal.post.domain.PostPlayer;
import com.dhoon.transfertracker.internal.post.domain.PostTeam;
import com.dhoon.transfertracker.internal.post.dto.response.PostItemResponseDto;
import com.dhoon.transfertracker.internal.post.dto.response.PostDetailResponseDto;
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
import com.google.api.gax.rpc.InvalidArgumentException;
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
                .map(this::toPostItem)
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
                .map(this::toPostItem)
                .toList();

        return PostsResponseDto.of(posts, postPage.getTotalElements(), postPage.getTotalPages(), postPage.hasNext(), postPage.hasPrevious());
    }

    public PostDetailResponseDto getPost(Long postId) {
        if (postId == null || postId <= 0) {
            throw new InvalidPostIdException();
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(NotFoundPostException::new);

        return PostDetailResponseDto.of(toPostItem(post));
    }

    public PostsResponseDto getTeamPosts(Long teamId, int page) {
        if (teamId == null || teamId <= 0) {
            throw new InvalidTeamIdException();
        }
        validatePageNotNegative(page);

        teamRepository.findById(teamId)
                .orElseThrow(NotFoundTeamException::new);

        Pageable pageable = createRelationPostPage(page);

        Page<PostTeam> postPage = postTeamRepository.findAllByTeamId(teamId, pageable);

        List<PostItemResponseDto> posts = postPage.getContent()
                .stream()
                .map(PostItemResponseDto::of)
                .toList();

        return PostsResponseDto.of(posts, postPage.getTotalElements(), postPage.getTotalPages(), postPage.hasNext(), postPage.hasPrevious());
    }

    public PostsResponseDto getPlayerPosts(Long playerId, int page) {
        if (playerId == null || playerId <= 0) {
            throw new InvalidPlayerIdException();
        }

        validatePageNotNegative(page);

        com.dhoon.transfertracker.internal.player.domain.Player player = playerRepository.findById(playerId)
                .orElseThrow(NotFoundPlayerException::new);

        Pageable relationPageable = createRelationPostPage(page);

        Page<PostPlayer> postPage = postPlayerRepository.findAllByPlayerId(playerId, relationPageable);

        if (postPage.hasContent()) {
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

        // Post 전환 이전에 연결 테이블이 비어 있던 게시물도 선수 상세에서 노출한다.
        // 기존 TransferPost를 다시 조회하지 않고, 현재 Post 본문을 기준으로만 보완한다.
        Page<Post> contentMatchedPostPage = postRepository.findAllByContentContainingIgnoreCase(
                player.getDisplayName(), createPostPage(page)
        );

        List<PostItemResponseDto> posts = contentMatchedPostPage.getContent()
                .stream()
                .map(post -> toPostItem(post, player))
                .toList();

        return PostsResponseDto.of(
                posts,
                contentMatchedPostPage.getTotalElements(),
                contentMatchedPostPage.getTotalPages(),
                contentMatchedPostPage.hasNext(),
                contentMatchedPostPage.hasPrevious()
        );
    }

    // ======================================================================================================== //
    private Pageable createPostPage(int page) {
        return PageRequest.of(
                page,
                30,
                Sort.by(Sort.Order.desc("contentCreatedAt"), Sort.Order.desc("id"))
        );
    }

    /**
     * PostTeam/PostPlayer 조회는 repository JPQL에서 post.contentCreatedAt 기준 정렬을 이미 지정한다.
     * Pageable에 Post 속성 정렬을 같이 전달하면 관계 엔티티에 해당 필드가 없어서 Hibernate가 실패한다.
     */
    private Pageable createRelationPostPage(int page) {
        return PageRequest.of(page, 30);
    }

    private PostItemResponseDto toPostItem(Post post) {
        PostTeam postTeam = postTeamRepository.findAllByPostId(post.getId())
                .stream()
                .findFirst()
                .orElse(null);
        PostPlayer postPlayer = postPlayerRepository.findAllByPostId(post.getId())
                .stream()
                .findFirst()
                .orElse(null);

        return PostItemResponseDto.of(post, postTeam, postPlayer);
    }

    private PostItemResponseDto toPostItem(Post post, com.dhoon.transfertracker.internal.player.domain.Player player) {
        PostTeam postTeam = postTeamRepository.findAllByPostId(post.getId())
                .stream()
                .findFirst()
                .orElse(null);

        return PostItemResponseDto.of(post, postTeam, player);
    }


    private void validatePageNotNegative(int page) {
        if (page < 0) {
            throw new InvalidPageException();
        }
    }


}
