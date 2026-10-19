package com.dhoon.transfertracker.internal.transferpost.sync.service.impl;

import com.dhoon.transfertracker.internal.transferpost.domain.TransferPost;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.transferpost.dto.response.TransferPostItemResponseDto;
import com.dhoon.transfertracker.internal.transferpost.repository.TransferPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
@Transactional
@Service
public class TransferPostSyncTxService {

    private final TransferPostRepository transferPostRepository;

    public TransferPostItemResponseDto saveTransferPost(TransferPostSource source, JsonNode post, Map<String, String> mediaUrlByKey) {
        String content = post.get("text").asString();
        boolean transferRelated = isTransferRelated(content);
        String postId = post.get("id").asString();
        Instant createdAt = Instant.parse(post.get("created_at").asString());
        String photoUrl = findPhotoUrl(post, mediaUrlByKey);

        TransferPost transferPost = transferPostRepository.findByExternalPostId(postId)
                .orElseGet(() ->
                        transferPostRepository.save(TransferPost.of(source, content, postId, createdAt, transferRelated, photoUrl))
                );

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

    private String findPhotoUrl(JsonNode post, Map<String, String> mediaUrlByKey) {
        JsonNode attachments = post.get("attachments");

        if (attachments == null) {
            return null;
        }

        JsonNode mediaKeys = attachments.get("media_keys");

        if (mediaKeys == null) {
            return null;
        }

        for (JsonNode mediaKeyNode : mediaKeys) {
            String mediaUrl = mediaUrlByKey.get(mediaKeyNode.asString());

            if (mediaUrl != null && !mediaUrl.isBlank()) {
                return mediaUrl;
            }
        }

        return null;
    }
}
