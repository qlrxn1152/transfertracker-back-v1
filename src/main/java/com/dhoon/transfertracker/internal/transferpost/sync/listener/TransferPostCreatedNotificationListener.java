package com.dhoon.transfertracker.internal.transferpost.sync.listener;

import com.dhoon.transfertracker.internal.notification.service.impl.MemberNotificationService;
import com.dhoon.transfertracker.internal.transferpost.sync.event.TransferPostCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransferPostCreatedNotificationListener {

    private final MemberNotificationService memberNotificationService;

    public void handle(TransferPostCreatedEvent event) {

        log.info("기자 소식 등록 이벤트 수신 postId = {}, journalist = {}",
                event.postId(),
                event.journalist()
        );

        memberNotificationService.sendTransferPostCreatedNotification(
                event.postId(),
                event.journalist()
        );
    }
}
