package com.dhoon.transfertracker.internal.notification.dto.request;

import com.dhoon.transfertracker.internal.notification.domain.FcmPlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MemberFcmTokenRequestDto(@NotNull FcmPlatform platform, @NotBlank String token) {
}
