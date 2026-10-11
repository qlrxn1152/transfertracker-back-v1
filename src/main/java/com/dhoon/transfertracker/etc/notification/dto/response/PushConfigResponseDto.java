package com.dhoon.transfertracker.etc.notification.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PushConfigResponseDto {

    private boolean enabled;
    private String publicKey;

    public static PushConfigResponseDto of(boolean enabled, String publicKey) {
        return new PushConfigResponseDto(enabled, enabled ? publicKey : null);
    }
}
