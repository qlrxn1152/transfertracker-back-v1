package com.dhoon.transfertracker.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true")
public class FirebaseConfig {

    private final String serviceAccountJson;

    public FirebaseConfig(@org.springframework.beans.factory.annotation.Value("${FIREBASE_SERVICE_ACCOUNT_JSON:}") String serviceAccountJson) {
        this.serviceAccountJson = serviceAccountJson;
    }

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        GoogleCredentials credentials = StringUtils.hasText(serviceAccountJson)
                ? GoogleCredentials.fromStream(new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8)))
                : GoogleCredentials.getApplicationDefault();

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build();

        return FirebaseApp.initializeApp(options);
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}
