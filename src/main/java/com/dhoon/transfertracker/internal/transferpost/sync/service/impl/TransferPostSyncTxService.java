package com.dhoon.transfertracker.internal.transferpost.sync.service.impl;

import com.dhoon.transfertracker.internal.transferpost.domain.TransferPost;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.transferpost.dto.response.TransferPostItemResponseDto;
import com.dhoon.transfertracker.internal.transferpost.repository.TransferPostRepository;
import com.dhoon.transfertracker.internal.transferpost.sync.event.TransferPostCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
@Slf4j
@Transactional
@Service
public class TransferPostSyncTxService {

    private final TransferPostRepository transferPostRepository;
    private final ApplicationEventPublisher eventPublisher;

    public TransferPostItemResponseDto saveTransferPost(
            TransferPostSource source,
            JsonNode post,
            Map<String, String> mediaUrlByKey,
            Map<String, JsonNode> referencedPostById
    ) {
        String content = post.get("text").asString();
        boolean transferRelated = isTransferRelated(content);
        String postId = post.get("id").asString();
        Instant createdAt = Instant.parse(post.get("created_at").asString());
        String photoUrl = findPhotoUrl(post, mediaUrlByKey, referencedPostById, new HashSet<>());

        TransferPost transferPost = transferPostRepository.findByExternalPostId(postId)
                .map(existingPost -> {
                    existingPost.updatePhotoUrlIfAbsent(photoUrl);
                    return existingPost;
                })
                .orElseGet(() -> {
                    TransferPost savedPost = transferPostRepository.save(TransferPost.of(source, content, postId, createdAt, transferRelated, photoUrl));

                    eventPublisher.publishEvent(new TransferPostCreatedEvent(
                            savedPost.getId(),
                            savedPost.getSourcer()
                    ));

                    return savedPost;
                });


        return TransferPostItemResponseDto.of(transferPost);
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

    private String getString(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        return value == null || value.isNull() ? null : value.asString();
    }
}
