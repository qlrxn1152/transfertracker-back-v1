package com.dhoon.transfertracker.external.fcm;

import com.google.firebase.messaging.FirebaseMessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * FCM 연결 확인을 위한 임시 테스트 API입니다.
 * Spring Security의 기본 정책(anyRequest authenticated)에 따라 로그인한 사용자만 호출할 수 있습니다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class FcmTestController {

    private final FcmNotificationService fcmNotificationService;

    @PostMapping("/api/fcm/test")
    public ResponseEntity<FcmTestResponse> sendTestNotification(@RequestBody FcmTestRequest request) {
        if (request == null || request.token() == null || request.token().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(FcmTestResponse.failure("FCM 등록 토큰이 필요합니다."));
        }

        try {
            String messageId = fcmNotificationService.sendTestNotification(request.token());
            return ResponseEntity.ok(FcmTestResponse.success(messageId));
        } catch (FirebaseMessagingException exception) {
            log.warn("FCM 테스트 알림 발송 실패: {}", exception.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(FcmTestResponse.failure("FCM 테스트 알림을 발송하지 못했습니다."));
        }
    }

    public record FcmTestRequest(String token) {
    }

    public record FcmTestResponse(boolean success, String message, String messageId) {
        static FcmTestResponse success(String messageId) {
            return new FcmTestResponse(true, "FCM 테스트 알림을 전송했습니다.", messageId);
        }

        static FcmTestResponse failure(String message) {
            return new FcmTestResponse(false, message, null);
        }
    }
}
