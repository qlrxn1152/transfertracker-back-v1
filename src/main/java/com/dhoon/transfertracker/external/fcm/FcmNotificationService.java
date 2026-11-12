package com.dhoon.transfertracker.external.fcm;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmNotificationService {

    private final ObjectProvider<FirebaseMessaging> firebaseMessagingProvider;


    public String sendTransferPostNotification(String token, String teamName) throws FirebaseMessagingException {
        FirebaseMessaging firebaseMessaging = firebaseMessagingProvider.getIfAvailable();
        if (firebaseMessaging == null) {
            log.warn("FCM is disabled. Skipping push notification for teamName={}", teamName);
            return null;
        }

        Notification notification = Notification.builder()
                .setTitle("TransferTracker")
                .setBody("[" + teamName + "] 팀에 새로운 소식이 등록되었습니다!")
                .build();

        Message message = Message.builder()
                .setToken(token)
                .setNotification(notification)
                .build();

        String messageId = firebaseMessaging.send(message);

        log.info(
                "FCM 발송 요청 성공. teamName={}, messageId={}",
                teamName,
                messageId
        );

        return messageId;
    }
}
