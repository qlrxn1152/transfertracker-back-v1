package com.dhoon.transfertracker.external.x.client;


import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPost;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.transferpost.repository.TransferPostRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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
        String username = source.getXUsername();
        String userId = source.getXUserId();

        JsonNode userPosts = getUserPosts(userId, 50).get("data");

        userPosts.forEach(post -> {
            String content = post.get("text").asString();
            boolean isTransferRelate = isTransferRelated(content);

            String postId = post.get("id").asString();

            Instant createdAt = Instant.parse(post.get("created_at").asString());

            transferPostRepository.findByExternalPostId(postId)
                    .orElseGet(() ->
                            transferPostRepository.save(TransferPost.of(
                                    source, content, postId, createdAt, isTransferRelate)
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
                        .queryParam("tweet.fields", "created_at")
                        .build(userId)
                )
                .retrieve()
                .body(JsonNode.class);
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
