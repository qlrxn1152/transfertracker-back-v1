package com.dhoon.transfertracker.external.x.client;


import com.dhoon.transfertracker.internal.domain.Transfer;
import com.dhoon.transfertracker.internal.domain.TransferPost;
import com.dhoon.transfertracker.internal.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.repository.TransferPostRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

//@RequiredArgsConstructor

@Slf4j
@Component
public class XClient {

    private final RestClient xRestClient;
    private final TransferPostRepository transferPostRepository;

    public XClient(@Qualifier("xRestClient") RestClient xRestClient, TransferPostRepository transferPostRepository) {
        this.xRestClient = xRestClient;
        this.transferPostRepository = transferPostRepository;
    }


    // 우선 로마노만 ..?
    public String syncPosts(TransferPostSource source) {
        String username = source.getXUsername();
        String userId = source.getXUserId();

        JsonNode userPosts = getUserPosts(userId).get("data");

        userPosts.forEach(post -> {
            String content = post.get("text").asString();
            String postId = post.get("id").asString();

            Instant createdAt = Instant.parse(post.get("created_at").asString());

            transferPostRepository.findByExternalPostId(postId)
                    .orElseGet(() -> transferPostRepository.save(TransferPost.of(
                            source, content, postId, createdAt
                    )));
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

    public JsonNode getUserPosts(String userId) {
        return xRestClient.get()
                .uri(uriBuilder -> uriBuilder.
                        path("/2/users/{id}/tweets")
                        .queryParam("max_results", 10)
                        .queryParam("tweet.fields", "created_at")
                        .build(userId)
                )
                .retrieve()
                .body(JsonNode.class);
    }





}
