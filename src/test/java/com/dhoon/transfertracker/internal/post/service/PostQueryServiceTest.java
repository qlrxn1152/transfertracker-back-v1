package com.dhoon.transfertracker.internal.post.service;

import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.exception.InvalidPlayerIdException;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.post.domain.Post;
import com.dhoon.transfertracker.internal.post.domain.PostJournalist;
import com.dhoon.transfertracker.internal.post.domain.PostPlayer;
import com.dhoon.transfertracker.internal.post.domain.PostTeam;
import com.dhoon.transfertracker.internal.post.dto.response.PostItemResponseDto;
import com.dhoon.transfertracker.internal.post.dto.response.PostsResponseDto;
import com.dhoon.transfertracker.internal.post.exception.InvalidPageException;
import com.dhoon.transfertracker.internal.post.query.service.PostQueryService;
import com.dhoon.transfertracker.internal.post.repository.PostPlayerRepository;
import com.dhoon.transfertracker.internal.post.repository.PostRepository;
import com.dhoon.transfertracker.internal.post.repository.PostTeamRepository;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.exception.InvalidTeamIdException;
import com.dhoon.transfertracker.internal.team.exception.NotFoundTeamException;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.teamplayer.domain.TeamPlayer;
import com.dhoon.transfertracker.internal.teamplayer.repository.TeamPlayerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class PostQueryServiceTest {

    @Mock private PostRepository postRepository;
    @Mock private PostTeamRepository postTeamRepository;
    @Mock private PostPlayerRepository postPlayerRepository;
    @Mock private TeamPlayerRepository teamPlayerRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private PlayerRepository playerRepository;

    @InjectMocks private PostQueryService queryService;

    private static Pageable getPageable() {
        return PageRequest.of(
                0,
                30,
                Sort.by(Sort.Order.desc("contentCreatedAt"), Sort.Order.desc("id"))
        );
    }

    @Test
    @DisplayName(value = "존재하는 게시물들을 조회할 수 있다.")
    void getPosts() throws Exception {
        // given
        LocalDateTime postCreatedAt = LocalDateTime.parse("2026-11-20T11:12:00");
        Pageable pageable = getPageable();

        Post post1 = Post.createPost(
                PostJournalist.FABRIZIO_ROMANO,
                "content-1",
                "external-id-1",
                false
        );
        ReflectionTestUtils.setField(post1, "contentCreatedAt", postCreatedAt);

        Post post2 = Post.createPost(
                PostJournalist.FABRIZIO_ROMANO,
                "content-2",
                "external-id-2",
                true
        );
        ReflectionTestUtils.setField(post2, "contentCreatedAt", postCreatedAt.minusDays(2));

        Page<Post> postPage = new PageImpl<>(
                List.of(post1, post2),
                pageable,
                2
        );

        given(postRepository.findAll(pageable))
                .willReturn(postPage);


        // when
        PostsResponseDto response = queryService.getPosts(0);

        // then
        assertThat(response.getPosts()).hasSize(2);
        assertThat(response.getPosts()).extracting(PostItemResponseDto::getExternalPostId)
                .containsExactly("external-id-1", "external-id-2");
        assertThat(response.getTotalPostCount()).isEqualTo(2);

        verify(postRepository).findAll(pageable);
    }

    @Test
    @DisplayName(value = "게시물이 존재하지 않는경우, 빈 리스트를 반환한다.")
    void getPosts_empty() throws Exception {
        // given
        Pageable pageable = getPageable();

        Page<Post> postPage = new PageImpl<>(
                List.of(),
                pageable,
                0
        );

        given(postRepository.findAll(pageable))
                .willReturn(postPage);

        // when
        PostsResponseDto response = queryService.getPosts(0);

        // then
        assertThat(response.getPosts()).isEmpty();
        assertThat(response.getTotalPostCount()).isEqualTo(0);
        verify(postRepository).findAll(pageable);
    }

    @Test
    @DisplayName(value = "page 의 값이 음수인경우 예외가 발생한다.")
    void getPosts_invalid_page() throws Exception {
        // given
        int invalidPage = -1;

        // when && then
        assertThatThrownBy(() -> queryService.getPosts(invalidPage))
                .isInstanceOf(InvalidPageException.class);

        verifyNoInteractions(postRepository);
    }

    @Test
    @DisplayName(value = "특정 기자의 게시물들을 조회할 수 있다.")
    void getJournalistPosts() throws Exception {
        // given
        LocalDateTime postCreatedAt = LocalDateTime.parse("2026-11-20T11:12:00");
        Pageable pageable = getPageable();

        Post post1 = Post.createPost(
                PostJournalist.FABRIZIO_ROMANO,
                "content-1",
                "external-id-1",
                false
        );
        ReflectionTestUtils.setField(post1, "contentCreatedAt", postCreatedAt);

        Post post2 = Post.createPost(
                PostJournalist.FABRIZIO_ROMANO,
                "content-2",
                "external-id-2",
                true
        );
        ReflectionTestUtils.setField(post2, "contentCreatedAt", postCreatedAt.minusDays(2));

        Page<Post> postPage = new PageImpl<>(
                List.of(post1, post2),
                pageable,
                2
        );

        given(postRepository.findAllByJournalist(PostJournalist.FABRIZIO_ROMANO, pageable))
                .willReturn(postPage);


        // when
        PostsResponseDto response = queryService.getJournalistPosts(PostJournalist.FABRIZIO_ROMANO, 0);

        // then
        assertThat(response.getPosts()).hasSize(2);
        assertThat(response.getPosts()).extracting(PostItemResponseDto::getExternalPostId)
                .containsExactly("external-id-1", "external-id-2");
        assertThat(response.getPosts()).extracting(PostItemResponseDto::getJournalist)
                .containsOnly(PostJournalist.FABRIZIO_ROMANO);
        assertThat(response.getTotalPostCount()).isEqualTo(2);

        verify(postRepository).findAllByJournalist(PostJournalist.FABRIZIO_ROMANO, pageable);
    }

    @Test
    @DisplayName(value = "특정 기자의 게시물이 존재하지않는경우, 빈 리스트를 반환한다.")
    void getJournalistPosts_empty() throws Exception {
        // given
        Pageable pageable = getPageable();

        Page<Post> postPage = new PageImpl<>(
                List.of(),
                pageable,
                0
        );

        given(postRepository.findAllByJournalist(PostJournalist.FABRIZIO_ROMANO, pageable))
                .willReturn(postPage);

        // when
        PostsResponseDto response = queryService.getJournalistPosts(PostJournalist.FABRIZIO_ROMANO, 0);
        // then
        assertThat(response.getPosts()).isEmpty();
        assertThat(response.getTotalPostCount()).isEqualTo(0);
        verify(postRepository).findAllByJournalist(PostJournalist.FABRIZIO_ROMANO, pageable);
    }

    @Test
    @DisplayName(value = "journalist 의 값이 Null 인 경우 예외가 발생한다.")
    void getJournalistPosts_invalid_journalist() throws Exception {
        // given
        PostJournalist nullJournalist = null;

        // when && then
        assertThatThrownBy(() -> queryService.getJournalistPosts(nullJournalist, 0))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(postRepository);
    }

    @Test
    @DisplayName(value = "page 의 값이 음수인 경우 예외가 발생한다.")
    void getJournalistPosts_negative_page() throws Exception {
        // given
        PostJournalist journalist = PostJournalist.FABRIZIO_ROMANO;
        int negativePage = -1;

        // when && then
        assertThatThrownBy(() -> queryService.getJournalistPosts(journalist, negativePage))
                .isInstanceOf(InvalidPageException.class);

        verifyNoInteractions(postRepository);
    }

    @Test
    @DisplayName(value = "특정 팀의 관련된 게시물들을 조회할 수 있다.")
    void getTeamPosts() throws Exception {
        // given
        Team team = Team.createTeam("teamA", 111L);
        LocalDateTime postCreatedAt = LocalDateTime.parse("2026-11-20T11:12:00");
        ReflectionTestUtils.setField(team, "id", 1L);


        given(teamRepository.findById(1L))
                .willReturn(Optional.of(team));

        Pageable pageable = getPageable();
        Post post1 = Post.createPost(
                PostJournalist.FABRIZIO_ROMANO,
                "content-1",
                "external-id-1",
                false
        );
        ReflectionTestUtils.setField(post1, "contentCreatedAt", postCreatedAt);

        Post post2 = Post.createPost(
                PostJournalist.FABRIZIO_ROMANO,
                "content-2",
                "external-id-2",
                true
        );
        ReflectionTestUtils.setField(post2, "contentCreatedAt", postCreatedAt.minusDays(2));

        PostTeam teamPost1 = PostTeam.postAssignTeam(team, post1);
        PostTeam teamPost2 = PostTeam.postAssignTeam(team, post2);

        Page<PostTeam> postPage = new PageImpl<>(
                List.of(teamPost1, teamPost2),
                pageable,
                0
        );

        given(postTeamRepository.findAllByTeamId(1L, pageable))
                .willReturn(postPage);


        // when
        PostsResponseDto response = queryService.getTeamPosts(1L, 0);

        // then
        assertThat(response.getPosts()).hasSize(2);
        assertThat(response.getPosts()).extracting(PostItemResponseDto::getExternalPostId)
                .containsExactly("external-id-1", "external-id-2");
        assertThat(response.getPosts()).extracting(PostItemResponseDto::getTeamName)
                .containsOnly("teamA");

        verify(teamRepository).findById(1L);
        verify(postTeamRepository).findAllByTeamId(1L, pageable);
    }

    @Test
    @DisplayName(value = "특정 팀의 게시물이 존재하지 않는경우, 빈 리스트를 반환한다.")
    void getTeamPosts_empty() throws Exception {
        // given
        Team team = Team.createTeam("teamA", 111L);
        ReflectionTestUtils.setField(team, "id", 1L);

        given(teamRepository.findById(1L))
                .willReturn(Optional.of(team));

        Pageable pageable = getPageable();

        Page<PostTeam> postPage = new PageImpl<>(
                List.of(),
                pageable,
                0
        );

        given(postTeamRepository.findAllByTeamId(1L, pageable))
                .willReturn(postPage);

        // when
        PostsResponseDto response = queryService.getTeamPosts(1L, 0);

        // then
        assertThat(response.getPosts()).isEmpty();

        verify(teamRepository).findById(1L);
        verify(postTeamRepository).findAllByTeamId(1L, pageable);
    }

    @Test
    @DisplayName(value = "teamId 의 값이 null 이거나, 0 이하인경우, 예외가 발생한다.")
    void getTeamPosts_invalid_teamId() throws Exception {
        // given
        Long nullTeamId = null;
        Long zeroTeamId = 0L;
        Long negativeTeamId = -1L;

        // when && then
        assertThatThrownBy(() -> queryService.getTeamPosts(nullTeamId, 0))
                .isInstanceOf(InvalidTeamIdException.class);

        assertThatThrownBy(() -> queryService.getTeamPosts(zeroTeamId, 0))
                .isInstanceOf(InvalidTeamIdException.class);

        assertThatThrownBy(() -> queryService.getTeamPosts(negativeTeamId, 0))
                .isInstanceOf(InvalidTeamIdException.class);

        verifyNoInteractions(teamRepository);
        verifyNoInteractions(postTeamRepository);
    }

    @Test
    @DisplayName(value = "page 의 값이 음수인경우, 예외가 발생한다.")
    void getTeamPosts_invalid_page() throws Exception {
        // given
        int negativePage = -2;

        // when && then
        assertThatThrownBy(() -> queryService.getTeamPosts(1L, negativePage))
                .isInstanceOf(InvalidPageException.class);

        verifyNoInteractions(teamRepository);
        verifyNoInteractions(postTeamRepository);
    }

    @Test
    @DisplayName(value = "존재하지 않는 팀을 조회한경우, 예외가 발생한다.")
    void getTeamPosts_notFoundTeam() throws Exception {
        // given
        given(teamRepository.findById(1L))
                .willReturn(Optional.empty());

        // when && then
        assertThatThrownBy(() -> queryService.getTeamPosts(1L, 0))
                .isInstanceOf(NotFoundTeamException.class);

        verify(teamRepository).findById(1L);
        verifyNoInteractions(postTeamRepository);
    }

    @Test
    @DisplayName(value = "특정 선수 ( FA ) 의 관련된 게시물들을 조회할 수 있다.")
    void getPlayerPosts_FA() throws Exception {
        // given
        Player player = Player.of("playerA", 111L);
        ReflectionTestUtils.setField(player, "id", 1L);

        LocalDateTime postCreatedAt = LocalDateTime.parse("2026-11-20T11:12:00");

        given(playerRepository.findById(1L))
                .willReturn(Optional.of(player));

        Pageable pageable = getPageable();
        Post post1 = Post.createPost(
                PostJournalist.FABRIZIO_ROMANO,
                "content-1",
                "external-id-1",
                false
        );
        ReflectionTestUtils.setField(post1, "contentCreatedAt", postCreatedAt);

        Post post2 = Post.createPost(
                PostJournalist.FABRIZIO_ROMANO,
                "content-2",
                "external-id-2",
                true
        );
        ReflectionTestUtils.setField(post2, "contentCreatedAt", postCreatedAt.minusDays(2));

        PostPlayer playerPost1 = PostPlayer.postAssignPlayer(player, post1);
        PostPlayer playerPost2 = PostPlayer.postAssignPlayer(player, post2);


        Page<PostPlayer> postPage = new PageImpl<>(
                List.of(playerPost1, playerPost2),
                pageable,
                0
        );

        given(postPlayerRepository.findAllByPlayerId(1L, pageable))
                .willReturn(postPage);


        // when
        PostsResponseDto response = queryService.getPlayerPosts(1L, 0);

        // then
        assertThat(response.getPosts()).hasSize(2);
        assertThat(response.getPosts()).extracting(PostItemResponseDto::getExternalPostId)
                .containsExactly("external-id-1", "external-id-2");
        assertThat(response.getPosts()).extracting(PostItemResponseDto::getPlayerName)
                .containsOnly("playerA");
        assertThat(response.getPosts()).extracting(PostItemResponseDto::getTeamName)
                        .containsOnlyNulls();

        verify(playerRepository).findById(1L);
        verify(postPlayerRepository).findAllByPlayerId(1L, pageable);
        verify(teamPlayerRepository).existsByPlayerId(1L);
        verify(teamPlayerRepository, never()).findByPlayerId(1L);
    }

    @Test
    @DisplayName(value = "특정 선수 ( TEAM PLAYER ) 의 관련된 게시물들을 조회할 수 있다.")
    void getPlayerPosts_TeamPlayer() throws Exception {
        // given
        Player player = Player.of("playerA", 111L);
        ReflectionTestUtils.setField(player, "id", 1L);

        given(playerRepository.findById(1L))
                .willReturn(Optional.of(player));

        Team team = Team.createTeam("teamA", 33L);
        TeamPlayer teamPlayer = TeamPlayer.of(team, player);

        given(teamPlayerRepository.existsByPlayerId(1L))
                .willReturn(true);
        given(teamPlayerRepository.findByPlayerId(1L))
                .willReturn(Optional.of(teamPlayer));

        LocalDateTime postCreatedAt = LocalDateTime.parse("2026-11-20T11:12:00");

        Pageable pageable = getPageable();
        Post post1 = Post.createPost(
                PostJournalist.FABRIZIO_ROMANO,
                "content-1",
                "external-id-1",
                false
        );
        ReflectionTestUtils.setField(post1, "contentCreatedAt", postCreatedAt);

        Post post2 = Post.createPost(
                PostJournalist.FABRIZIO_ROMANO,
                "content-2",
                "external-id-2",
                true
        );
        ReflectionTestUtils.setField(post2, "contentCreatedAt", postCreatedAt.minusDays(2));

        PostPlayer playerPost1 = PostPlayer.postAssignPlayer(player, post1);
        PostPlayer playerPost2 = PostPlayer.postAssignPlayer(player, post2);

        Page<PostPlayer> postPage = new PageImpl<>(
                List.of(playerPost1, playerPost2),
                pageable,
                0
        );

        given(postPlayerRepository.findAllByPlayerId(1L, pageable))
                .willReturn(postPage);

        // when
        PostsResponseDto response = queryService.getPlayerPosts(1L, 0);

        // then
        assertThat(response.getPosts()).hasSize(2);
        assertThat(response.getPosts()).extracting(PostItemResponseDto::getExternalPostId)
                .containsExactly("external-id-1", "external-id-2");
        assertThat(response.getPosts()).extracting(PostItemResponseDto::getPlayerName)
                .containsOnly("playerA");
        assertThat(response.getPosts()).extracting(PostItemResponseDto::getTeamName)
                .containsOnly("teamA");

        verify(playerRepository).findById(1L);
        verify(postPlayerRepository).findAllByPlayerId(1L, pageable);
        verify(teamPlayerRepository).existsByPlayerId(1L);
        verify(teamPlayerRepository).findByPlayerId(1L);
    }

    @Test
    @DisplayName(value = "playerId 의 값이 null 이거나, 0 이하인경우 예외가 발생한다.")
    void getPlayerPosts_invalid_playerId() throws Exception {
        // given
        Long nullPlayerId = null;
        Long zeroPlayerId = 0L;
        Long negativePlayerId = -1L;

        // when && then
        assertThatThrownBy(() -> queryService.getPlayerPosts(nullPlayerId, 0))
                .isInstanceOf(InvalidPlayerIdException.class);
        assertThatThrownBy(() -> queryService.getPlayerPosts(zeroPlayerId, 0))
                .isInstanceOf(InvalidPlayerIdException.class);
        assertThatThrownBy(() -> queryService.getPlayerPosts(negativePlayerId, 0))
                .isInstanceOf(InvalidPlayerIdException.class);

        verifyNoInteractions(playerRepository);
        verifyNoInteractions(postPlayerRepository);
        verifyNoInteractions(teamPlayerRepository);
    }

    @Test
    @DisplayName(value = "page 의 값이 음수인경우, 예외가 발생한다.")
    void getPlayerPosts_invalid_page() throws Exception {
        // given
        int negativePage = -2;

        // when && then
        assertThatThrownBy(() -> queryService.getPlayerPosts(1L, negativePage))
                .isInstanceOf(InvalidPageException.class);


        verifyNoInteractions(playerRepository);
        verifyNoInteractions(postPlayerRepository);
        verifyNoInteractions(teamPlayerRepository);
    }






}
