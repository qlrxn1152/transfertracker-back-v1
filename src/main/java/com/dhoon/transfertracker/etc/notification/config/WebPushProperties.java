package com.dhoon.transfertracker.etc.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "webpush.vapid")
public record WebPushProperties(
        String publicKey,
        String privateKey,
        String subject
) {

    public boolean enabled() {
        return hasText(publicKey) && hasText(privateKey);
    }

    public String subjectOrDefault() {
        return hasText(subject) ? subject : "mailto:qlrxn1152@naver.com";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
