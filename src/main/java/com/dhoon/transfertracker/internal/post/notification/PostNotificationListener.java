package com.dhoon.transfertracker.internal.post.notification;

import com.dhoon.transfertracker.internal.notification.service.impl.MemberNotificationService;
import com.dhoon.transfertracker.internal.post.event.PostCreatedEvent;
import com.dhoon.transfertracker.internal.post.event.PostTeamAssignedEvent;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PostNotificationListener {

    private final MemberNotificationService memberNotificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PostCreatedEvent event) {
        memberNotificationService.sendTransferPostCreatedNotification(
                event.postId(),
                TransferPostSource.valueOf(event.journalist().name())
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PostTeamAssignedEvent event) {
        memberNotificationService.sendTransferPostNotification(event.postId(), event.teamId());
    }
}
