package com.dhoon.transfertracker.external.x.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class XConfig {

    @Bean("xRestClient")
    public RestClient xClient(
            RestClient.Builder builder,
            @Value("${x.api.base-url}") String baseUrl,
            @Value("${x.api.bearer-token}") String bearerToken
    ) {
        return builder
                .baseUrl(baseUrl)
                .defaultHeaders(headers -> headers.setBearerAuth(bearerToken))
                .build();
    }
}
