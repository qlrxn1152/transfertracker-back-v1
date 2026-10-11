package com.dhoon.transfertracker.external.x.client;


import com.dhoon.transfertracker.internal.transferpost.domain.TransferPost;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.transferpost.repository.TransferPostRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class XClient {

    private final RestClient xRestClient;
    private final TransferPostRepository transferPostRepository;

    public XClient(@Qualifier("xRestClient") RestClient xRestClient, TransferPostRepository transferPostRepository) {
        this.xRestClient = xRestClient;
        this.transferPostRepository = transferPostRepository;
    }



    public String syncPosts(TransferPostSource source) {
        String userId = source.getXUserId();

        JsonNode response = getUserPosts(userId, 50);
        JsonNode userPosts = response.get("data");
        Map<String, String> mediaUrlByKey = mapMediaUrlByKey(response);

        if (userPosts == null) {
            return "OK";
        }

        userPosts.forEach(post -> {
            String content = post.get("text").asString();
            boolean isTransferRelate = isTransferRelated(content);

            String postId = post.get("id").asString();
            String photoUrl = findPhotoUrl(post, mediaUrlByKey);

            Instant createdAt = Instant.parse(post.get("created_at").asString());

            transferPostRepository.findByExternalPostId(postId)
                    .orElseGet(() ->
                            transferPostRepository.save(TransferPost.of(
                                    source, content, postId, createdAt, isTransferRelate, photoUrl)
                            ));
        });

        return "OK";
    }




    public JsonNode getUserByUsername(String username) {
        return xRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/2/users/by/username/{username}")
                        .build(username)
                )
                .retrieve()
                .body(JsonNode.class);
    }

    public JsonNode getUserPosts(String userId, int size) {
        return xRestClient.get()
                .uri(uriBuilder -> uriBuilder.
                        path("/2/users/{id}/tweets")
                        .queryParam("max_results", size) // 1 <= size <= 100 ( size => 자연수 )
                        .queryParam("tweet.fields", "created_at,attachments")
                        .queryParam("expansions", "attachments.media_keys")
                        .queryParam("media.fields", "media_key,type,url,preview_image_url,width,height")
                        .build(userId)
                )
                .retrieve()
                .body(JsonNode.class);
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

    private String getString(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);

        if (value == null) {
            return null;
        }

        return value.asString();
    }




    public boolean isTransferRelated(String content) {
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





}
