package com.dhoon.transfertracker.internal.post.record;

public record PostGenerationTarget(
        String externalPostId,
        String journalistName,
        String content
) {
}
