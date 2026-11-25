package com.dhoon.transfertracker.external.x.client;


import com.dhoon.transfertracker.internal.post.domain.PostJournalist;
import com.dhoon.transfertracker.monitoring.ExternalApiMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;


@Slf4j
@Component
public class XClient {

    private final RestClient xRestClient;
    private final ObjectProvider<ExternalApiMetrics> externalApiMetricsProvider;

    public XClient(
            @Qualifier("xRestClient") RestClient xRestClient,
            ObjectProvider<ExternalApiMetrics> externalApiMetricsProvider
    ) {
        this.xRestClient = xRestClient;
        this.externalApiMetricsProvider = externalApiMetricsProvider;
    }

    public JsonNode getUserByUsername(String username) {
        return record("user_lookup", () -> xRestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/2/users/by/username/{username}").build(username))
                .retrieve().body(JsonNode.class));
    }

    public JsonNode getUserPosts(String userId, int size, String paginationToken) {
        return record("user_posts", () -> xRestClient.get()
                .uri(uriBuilder -> {
                    uriBuilder
                            .path("/2/users/{id}/tweets")
                            .queryParam("max_results", size)
                            .queryParam("tweet.fields", "created_at,attachments,referenced_tweets")
                            .queryParam("expansions", "attachments.media_keys,referenced_tweets.id,referenced_tweets.id.attachments.media_keys")
                            .queryParam("media.fields",
                                    "media_key,type,url,preview_image_url,width,height");

                    if (paginationToken != null && !paginationToken.isBlank()) {
                        uriBuilder.queryParam("pagination_token", paginationToken);
                    }

                    return uriBuilder.build(userId);
                })
                .retrieve()
                .body(JsonNode.class));
    }

    public JsonNode getJournalistPosts(PostJournalist journalist) {
        return getUserPosts(journalist.getXUserId(), 10, null);
    }

    private <T> T record(String operation, java.util.function.Supplier<T> call) {
        ExternalApiMetrics metrics = externalApiMetricsProvider.getIfAvailable();
        return metrics == null ? call.get() : metrics.record("x", operation, call);
    }










}
