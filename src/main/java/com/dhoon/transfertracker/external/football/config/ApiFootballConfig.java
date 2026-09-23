package com.dhoon.transfertracker.external.football.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ApiFootballConfig {

    @Bean
    public RestClient ApiFootballClient(
            RestClient.Builder builder,
            @Value("${football.api.base-url}") String baseUrl,
            @Value("${football.api.key}") String apiKey
    ) {
        return builder
                .baseUrl(baseUrl)
                .defaultHeader("x-apisports-key", apiKey)
                .build();
    }

}
