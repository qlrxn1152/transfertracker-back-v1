package com.dhoon.transfertracker.internal.transferpost.refa;

import com.dhoon.transfertracker.external.openai.client.OpenAiClient;
import com.dhoon.transfertracker.external.openai.dto.request.TranslationTarget;
import com.dhoon.transfertracker.external.openai.dto.response.TranslationBatchResult;
import com.dhoon.transfertracker.external.openai.dto.response.TranslationResult;
import com.dhoon.transfertracker.external.x.client.XClient;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.transferpost.command.dto.TeamAssignPostResult;
import com.dhoon.transfertracker.internal.transferpost.command.dto.TeamAssignPostTarget;
import com.dhoon.transfertracker.internal.transferpost.command.dto.TeamAssignPostsResult;
import com.dhoon.transfertracker.internal.transferpost.command.dto.TeamAssignPostsTarget;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPost;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostPlayer;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostTeam;
import com.dhoon.transfertracker.internal.transferpost.exception.NotFoundTransferPostException;
import com.dhoon.transfertracker.internal.transferpost.repository.TransferPostPlayerRepository;
import com.dhoon.transfertracker.internal.transferpost.repository.TransferPostRepository;
import com.dhoon.transfertracker.internal.transferpost.repository.TransferPostTeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class TransferPostRefa {

    private final XClient xClient;
    private final OpenAiClient openAiClient;

    private final TransferPostRepository transferPostRepository;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;

    private final TransferPostTeamRepository transferPostTeamRepository;
    private final TransferPostPlayerRepository transferPostPlayerRepository;

    public String syncTransferPost(TransferPostSource source) {

        JsonNode response = xClient.callExternalApiRefa(source);
        JsonNode userPosts = response.get("data");
        Map<String, String> mediaUrlByKey = mapMediaUrlByKey(response);
        Map<String, JsonNode> referencedPostById = mapReferencedPostById(response);

        List<TeamAssignPostTarget> assignPostTargets = new ArrayList<>();
        List<TranslationTarget> translationTargets = new ArrayList<>();


        // 해당 게시물들 먼저 새로 저장.
        userPosts.forEach(post -> {

            String content = post.get("text").asString();
            String externalPostId = post.get("id").asString();
            Instant createdAt = Instant.parse(post.get("created_at").asString());
            boolean transferRelated = isTransferRelated(content);
            String photoUrl = findPhotoUrl(post, mediaUrlByKey, referencedPostById, new HashSet<>());

            TransferPost savedTransferPost = transferPostRepository.findByExternalPostId(externalPostId)
                    .orElseGet(() -> transferPostRepository.save(TransferPost.of(
                            source,
                            content,
                            externalPostId,
                            createdAt,
                            transferRelated,
                            photoUrl
                    )));

            TeamAssignPostTarget assignPostTarget = new TeamAssignPostTarget(savedTransferPost.getId(), content);

            assignPostTargets.add(assignPostTarget);
            translationTargets.add(new TranslationTarget(savedTransferPost.getId(), content));

        });

        // 저장된 포스트들대상으로
        TeamAssignPostsResult assignedResults = openAiClient.classifyTeamsAndPlayers(new TeamAssignPostsTarget(assignPostTargets));

        // 포스트에 관련된 팀과 플레이어 할당
        for (TeamAssignPostResult assignResult : assignedResults.posts()) {
            Long postId = assignResult.postId();

            TransferPost transferPost = transferPostRepository.findById(postId)
                    .orElseThrow(NotFoundTransferPostException::new);

            assignResult.teamNames()
                    .forEach(teamName ->
                        teamRepository.findByTeamName(teamName)
                                .ifPresent(
                                        team -> transferPostTeamRepository.save(TransferPostTeam.postAssignTeam(team, transferPost)))
                    );

            assignResult.playerNames()
                    .forEach(playerName ->
                            playerRepository.findByPlayerName(playerName)
                                    .ifPresent(
                                            player -> transferPostPlayerRepository.save(TransferPostPlayer.postAssignPlayer(player, transferPost))
                                    )
                            );
        }


        TranslationBatchResult translatedResults = openAiClient.translate(translationTargets);

        // 포스트 영문내용 한국어로 번역
        for (TranslationResult translationResult : translatedResults.getPosts()) {
            Long postId = translationResult.getPostId();
            String translatedContent = translationResult.getTranslatedContent();

            transferPostRepository.findById(postId)
                    .ifPresent(transferPost -> transferPost.translate(translatedContent));
        }

        return "성공";
    }


    // ========================================================== //
    private boolean isTransferRelated(String content) {
        List<String> keyWords = List.of(
                "transfer",
                "deal",
                "agreement",
                "agreed",
                "bid",
                "offer",
                "talks",
                "personal terms",
                "signing",
                "join",
                "loan",
                "medical",
                "contract",
                "extension",
                "renew",
                "here we go"
        );

        String targetContent = content.toLowerCase();

        return keyWords.stream()
                .anyMatch(targetContent::contains);
    }

    private Map<String, String> mapMediaUrlByKey(JsonNode response) {
        Map<String, String> mediaUrlByKey = new HashMap<>();
        JsonNode includes = response.get("includes");

        if (includes == null) {
            return mediaUrlByKey;
        }

        JsonNode mediaList = includes.get("media");

        if (mediaList == null) {
            return mediaUrlByKey;
        }

        mediaList.forEach(media -> {
            String mediaKey = getString(media, "media_key");
            String type = getString(media, "type");

            if (mediaKey == null || type == null) {
                return;
            }

            String mediaUrl =
                    "photo".equals(type)
                            ? getString(media, "url")
                            : getString(media, "preview_image_url");

            if (mediaUrl != null && !mediaUrl.isBlank()) {
                mediaUrlByKey.put(mediaKey, mediaUrl);
            }
        });

        return mediaUrlByKey;
    }


    private String getString(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);

        if (value == null) {
            return null;
        }

        return value.asString();
    }

    private Map<String, JsonNode> mapReferencedPostById(JsonNode response) {
        Map<String, JsonNode> referencedPostById = new HashMap<>();
        JsonNode tweets = response.path("includes").get("tweets");

        if (tweets == null || !tweets.isArray()) {
            return referencedPostById;
        }

        tweets.forEach(tweet -> {
            String id = getString(tweet, "id");
            if (id != null && !id.isBlank()) {
                referencedPostById.put(id, tweet);
            }
        });

        return referencedPostById;
    }

    private String findPhotoUrl(
            JsonNode post,
            Map<String, String> mediaUrlByKey,
            Map<String, JsonNode> referencedPostById,
            Set<String> visitedPostIds
    ) {
        String postId = getString(post, "id");
        if (postId != null && !visitedPostIds.add(postId)) {
            return null;
        }

        JsonNode attachments = post.get("attachments");

        if (attachments != null) {
            JsonNode mediaKeys = attachments.get("media_keys");

            if (mediaKeys != null) {
                for (JsonNode mediaKeyNode : mediaKeys) {
                    String mediaUrl = mediaUrlByKey.get(mediaKeyNode.asString());

                    if (mediaUrl != null && !mediaUrl.isBlank()) {
                        return mediaUrl;
                    }
                }
            }
        }

        JsonNode referencedTweets = post.get("referenced_tweets");
        if (referencedTweets != null && referencedTweets.isArray()) {
            for (JsonNode reference : referencedTweets) {
                JsonNode referencedPost = referencedPostById.get(getString(reference, "id"));
                if (referencedPost == null) {
                    continue;
                }

                String referencedPhotoUrl = findPhotoUrl(
                        referencedPost,
                        mediaUrlByKey,
                        referencedPostById,
                        visitedPostIds
                );
                if (referencedPhotoUrl != null && !referencedPhotoUrl.isBlank()) {
                    return referencedPhotoUrl;
                }
            }
        }

        return null;
    }


}
