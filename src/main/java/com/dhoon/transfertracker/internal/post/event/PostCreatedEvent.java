package com.dhoon.transfertracker.internal.post.event;

import com.dhoon.transfertracker.internal.post.domain.PostJournalist;

public record PostCreatedEvent(Long postId, PostJournalist journalist) {
}
