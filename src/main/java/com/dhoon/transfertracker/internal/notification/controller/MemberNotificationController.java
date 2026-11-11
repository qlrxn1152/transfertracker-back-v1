package com.dhoon.transfertracker.internal.notification.controller;

import com.dhoon.transfertracker.internal.notification.service.impl.MemberNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class MemberNotificationController {

    private final MemberNotificationService notificationService;

    @PostMapping("/api/test/notifications/{memberId}")
    public ResponseEntity<Void> sendTest(@PathVariable Long memberId) {
        notificationService.sendTestNotification(memberId);
        return ResponseEntity.ok().build();
    }


}
