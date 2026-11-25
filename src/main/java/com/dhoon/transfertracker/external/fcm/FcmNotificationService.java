package com.dhoon.transfertracker.external.fcm;

import com.dhoon.transfertracker.internal.post.domain.PostJournalist;
import com.dhoon.transfertracker.monitoring.NotificationMetrics;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmNotificationService {

    private final ObjectProvider<FirebaseMessaging> firebaseMessagingProvider;
    private final NotificationMetrics notificationMetrics;

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public String sendTeamPostNotification(String token, String teamName, Long postId) throws FirebaseMessagingException {
        FirebaseMessaging firebaseMessaging = firebaseMessagingProvider.getIfAvailable();
        if (firebaseMessaging == null) {
            log.warn("FCM is disabled. Skipping push notification for teamName={}", teamName);
            return null;
        }

        Notification notification = Notification.builder()
                .setTitle("VEYRO")
                .setBody("[" + teamName + "] 팀에 새로운 소식이 등록됐습니다!")
                .build();

        Message message = Message.builder()
                .setToken(token)
                .setNotification(notification)
                .putData("type", "NEW_POST")
                .putData("postId", String.valueOf(postId))
                .build();

        String messageId = send(firebaseMessaging, message, "team");

        log.info(
                "FCM 발송 요청 성공. teamName={}, messageId={}",
                teamName,
                messageId
        );

        return messageId;
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public String sendJournalistPostCreatedNotification(PostJournalist journalist, String token, Long postId) throws FirebaseMessagingException {
        FirebaseMessaging firebaseMessaging = firebaseMessagingProvider.getIfAvailable();

        if (firebaseMessaging == null) {
            log.warn("FCM is disabled. Skipping push notification for Journalist={}", journalist);
            return null;
        }

        Notification notification = Notification.builder()
                .setTitle("VEYRO")
                .setBody("[" + journalist.getDisplayerName() + "] 기자의 새로운 소식이 등록됐습니다!")
                .build();

        Message message = Message.builder()
                .setToken(token)
                .setNotification(notification)
                .putData("type", "NEW_POST")
                .putData("postId", String.valueOf(postId))
                .build();

        String messageId = send(firebaseMessaging, message, "journalist");

        log.info(
                "FCM 발송 요청 성공. journalist={}, messageId={}",
                journalist,
                messageId
        );

        return messageId;
    }

    public String sendPlayerPostNotification(String token, String playerName, Long postId) throws FirebaseMessagingException {
        FirebaseMessaging firebaseMessaging = firebaseMessagingProvider.getIfAvailable();
        if (firebaseMessaging == null) {
            log.warn("FCM is disabled. Skipping push notification for playerName={}", playerName);
            return null;
        }

        Notification notification = Notification.builder()
                .setTitle("VEYRO")
                .setBody("[" + playerName + "] 선수에 새로운 소식이 등록됐습니다!")
                .build();

        Message message = Message.builder()
                .setToken(token)
                .setNotification(notification)
                .putData("type", "NEW_POST")
                .putData("postId", String.valueOf(postId))
                .build();

        String messageId = send(firebaseMessaging, message, "player");

        log.info(
                "FCM 발송 요청 성공. playerName={}, messageId={}",
                playerName,
                messageId
        );

        return messageId;

    }

    private String send(FirebaseMessaging firebaseMessaging, Message message, String channel) throws FirebaseMessagingException {
        try {
            String messageId = firebaseMessaging.send(message);
            notificationMetrics.success(channel);
            return messageId;
        } catch (FirebaseMessagingException exception) {
            notificationMetrics.failure(channel);
            throw exception;
        }
    }
}
