package com.dhoon.transfertracker.internal.notification.listener;

import com.dhoon.transfertracker.internal.notification.service.impl.MemberNotificationService;
import com.dhoon.transfertracker.internal.transferpost.command.event.TransferPostTeamAssignedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class TransferPostNotificationListener {

    private final MemberNotificationService memberNotificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(TransferPostTeamAssignedEvent event) {
        memberNotificationService.sendTransferPostNotification(
                event.transferPostId(),
                event.teamId()
        );

    }
}
