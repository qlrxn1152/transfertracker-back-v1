package com.dhoon.transfertracker.etc.notification.controller;

import com.dhoon.transfertracker.etc.notification.dto.request.MemberPushSubscriptionRequestDto;
import com.dhoon.transfertracker.etc.notification.dto.response.MemberNotificationsResponseDto;
import com.dhoon.transfertracker.etc.notification.dto.response.PushConfigResponseDto;
import com.dhoon.transfertracker.etc.notification.service.MemberNotificationService;
import com.dhoon.transfertracker.etc.notification.service.MemberPushNotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class MemberNotificationController {

    private final MemberNotificationService memberNotificationService;
    private final MemberPushNotificationService memberPushNotificationService;

    @GetMapping("/api/member/notifications")
    public ResponseEntity<MemberNotificationsResponseDto> getNotifications(@AuthenticationPrincipal Jwt jwt) {
        MemberNotificationsResponseDto response = memberNotificationService.getNotifications(Long.valueOf(jwt.getSubject()));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/api/member/notifications/refresh")
    public ResponseEntity<MemberNotificationsResponseDto> refreshNotifications(@AuthenticationPrincipal Jwt jwt) {
        MemberNotificationsResponseDto response = memberNotificationService.refreshNotifications(Long.valueOf(jwt.getSubject()));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PatchMapping("/api/member/notifications/{notificationId}/read")
    public ResponseEntity<Void> markRead(@AuthenticationPrincipal Jwt jwt, @PathVariable Long notificationId) {
        memberNotificationService.markRead(Long.valueOf(jwt.getSubject()), notificationId);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PatchMapping("/api/member/notifications/read-all")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal Jwt jwt) {
        memberNotificationService.markAllRead(Long.valueOf(jwt.getSubject()));

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/api/member/push/config")
    public ResponseEntity<PushConfigResponseDto> getPushConfig() {
        return ResponseEntity.status(HttpStatus.OK).body(memberPushNotificationService.getConfig());
    }

    @PostMapping("/api/member/push/subscriptions")
    public ResponseEntity<Void> subscribePush(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody MemberPushSubscriptionRequestDto request
    ) {
        memberPushNotificationService.subscribe(Long.valueOf(jwt.getSubject()), request);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/api/member/push/subscriptions")
    public ResponseEntity<Void> unsubscribePush(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody MemberPushSubscriptionRequestDto request
    ) {
        memberPushNotificationService.unsubscribe(Long.valueOf(jwt.getSubject()), request);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
