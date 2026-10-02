package com.dhoon.transfertracker.external.openai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@Configuration
public class OpenAiConfig {

    @Bean("openAiRestClient")
    public RestClient openAiRestClient(
            RestClient.Builder builder,
            @Value("${openai.api.base-url}") String baseUrl,
            @Value("${openai.api.key}") String apiKey
    ) {
        return builder
                .baseUrl(baseUrl)
                .defaultHeaders(headers -> {
                    headers.setBearerAuth(apiKey);
                    headers.setContentType(MediaType.APPLICATION_JSON);
                })
                .build();
    }

}
