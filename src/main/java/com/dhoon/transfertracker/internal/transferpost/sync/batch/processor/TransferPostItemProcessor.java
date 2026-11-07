package com.dhoon.transfertracker.internal.transferpost.sync.batch.processor;

import com.dhoon.transfertracker.internal.transferpost.domain.TransferPost;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.transferpost.sync.batch.dto.TransferPostRawItem;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class TransferPostItemProcessor implements ItemProcessor<TransferPostRawItem, TransferPost> {

    @Override
    public @Nullable TransferPost process(TransferPostRawItem item) throws Exception {
        TransferPostSource source = item.source();
        JsonNode post = item.post();
        Map<String, String> mediaUrlByKey = item.mediaUrlByKey();

        String content = post.get("text").asString();
        boolean transferRelated = isTransferRelated(content);
        String postId = post.get("id").asString();
        Instant createdAt = Instant.parse(post.get("created_at").asString());
        String photoUrl = findPhotoUrl(post, mediaUrlByKey);

        return TransferPost.of(source, content, postId, createdAt, transferRelated, photoUrl);
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
