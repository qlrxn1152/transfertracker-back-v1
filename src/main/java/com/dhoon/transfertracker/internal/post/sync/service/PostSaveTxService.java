package com.dhoon.transfertracker.internal.post.sync.service;

import com.dhoon.transfertracker.external.openai.dto.response.TranslationResult;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.post.domain.Post;
import com.dhoon.transfertracker.internal.post.domain.PostJournalist;
import com.dhoon.transfertracker.internal.post.domain.PostPlayer;
import com.dhoon.transfertracker.internal.post.domain.PostTeam;
import com.dhoon.transfertracker.internal.post.exception.NotFoundPostException;
import com.dhoon.transfertracker.internal.post.record.PostGenerationResult;
import com.dhoon.transfertracker.internal.post.record.TeamAssignPostResult;
import com.dhoon.transfertracker.internal.post.repository.PostPlayerRepository;
import com.dhoon.transfertracker.internal.post.repository.PostRepository;
import com.dhoon.transfertracker.internal.post.repository.PostTeamRepository;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class PostSaveTxService {

    private final PostRepository postRepository;

    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;

    private final PostTeamRepository postTeamRepository;
    private final PostPlayerRepository postPlayerRepository;

    public Post savePost(PostGenerationResult generatedPost, PostJournalist journalist) {

        String externalPostId = generatedPost.externalPostId();
        String generationContent = generatedPost.generationContent();
        Boolean transferRelated = generatedPost.transferRelated();

        return postRepository.findByExternalPostId(externalPostId)
                .orElseGet(() -> postRepository.save(Post.createPost(
                                journalist,
                                generationContent,
                                externalPostId,
                                transferRelated
                        ))
                );
    }

    public void assignTeamPlayer(TeamAssignPostResult assignResult) {
        Long postId = assignResult.postId();

        Post post = postRepository.findById(postId)
                .orElseThrow(NotFoundPostException::new);

        assignResult.teamNames()
                .forEach(teamName ->
                        teamRepository.findByTeamName(teamName)
                                .ifPresent(
                                        team -> postTeamRepository.findByTeamIdAndPostId(team.getId(), post.getId())
                                                .orElseGet(() -> postTeamRepository.save(PostTeam.postAssignTeam(team, post))))
                );

        assignResult.playerNames()
                .forEach(playerName ->
                        playerRepository.findByPlayerName(playerName)
                                .ifPresent(
                                        player -> postPlayerRepository.findByPlayerIdAndPostId(player.getId(), post.getId())
                                                .orElseGet(() -> postPlayerRepository.save(PostPlayer.postAssignPlayer(player, post)))
                                )
                );
    }

    public void translateContent(TranslationResult translationResult) {
        Long postId = translationResult.getPostId();
        String translatedText = translationResult.getTranslatedContent();

        postRepository.findById(postId)
                .ifPresent(post -> post.translate(translatedText));
    }

    public boolean checkExistPost(String externalPostId) {
        return postRepository.existsByExternalPostId(externalPostId);
    }
}
