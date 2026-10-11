package com.dhoon.transfertracker.etc.notification.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberPushSubscriptionRequestDto {

    @NotBlank
    private String endpoint;

    private Long expirationTime;

    @Valid
    @NotNull
    private Keys keys;

    public String getP256dh() {
        return keys == null ? null : keys.getP256dh();
    }

    public String getAuth() {
        return keys == null ? null : keys.getAuth();
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Keys {

        @NotBlank
        private String p256dh;

        @NotBlank
        private String auth;
    }
}
