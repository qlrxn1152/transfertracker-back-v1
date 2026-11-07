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

    public JsonNode callExternalApi(TransferPostSource source, String paginationToken) {
        String userId = source.getXUserId();
        return getUserPosts(userId, 50, paginationToken);
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

    public JsonNode getUserPosts(String userId, int size, String paginationToken) {
        return xRestClient.get()
                .uri(uriBuilder -> {
                    uriBuilder
                            .path("/2/users/{id}/tweets")
                            .queryParam("max_results", size)
                            .queryParam("tweet.fields", "created_at,attachments")
                            .queryParam("expansions", "attachments.media_keys")
                            .queryParam("media.fields",
                                    "media_key,type,url,preview_image_url,width,height");

                    if (paginationToken != null && !paginationToken.isBlank()) {
                        uriBuilder.queryParam("pagination_token", paginationToken);
                    }

                    return uriBuilder.build(userId);
                })
                .retrieve()
                .body(JsonNode.class);
    }










}
