package com.dhoon.transfertracker.internal.transferpost.sync.event;

import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;

public record TransferPostCreatedEvent(
        Long postId,
        TransferPostSource journalist
) {
}
