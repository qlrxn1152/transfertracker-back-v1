package com.dhoon.transfertracker.internal.notification.listener;

import com.dhoon.transfertracker.internal.notification.service.impl.MemberNotificationService;
import com.dhoon.transfertracker.internal.transferpost.command.event.TransferPostTeamAssignedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransferPostNotificationListener {

    private final MemberNotificationService memberNotificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) // 커밋이후 알림발송
    public void handle(TransferPostTeamAssignedEvent event) {
        log.info("관심 팀 알림 이벤트 수신 postId = {}, teamId = {}", event.transferPostId(), event.teamId());

        memberNotificationService.sendTransferPostNotification(
                event.transferPostId(),
                event.teamId()
        );

    }
}
