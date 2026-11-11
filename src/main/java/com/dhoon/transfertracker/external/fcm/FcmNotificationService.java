package com.dhoon.transfertracker.external.fcm;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FcmNotificationService {

    private final FirebaseMessaging firebaseMessaging;

    public String sendTestNotification(String token) throws FirebaseMessagingException {

        Notification notification = Notification.builder()
                .setTitle("TransferTracker")
                .setBody("관심 팀의 새로운 이적 소식이 등록되었습니다!")
                .build();

        Message message = Message.builder()
                .setToken(token)
                .setNotification(notification)
                .build();

        return firebaseMessaging.send(message);
    }

    public String sendTransferPostNotification(String token, String teamName) throws FirebaseMessagingException {
        Notification notification = Notification.builder()
                .setTitle("TransferTracker")
                .setBody("[" + teamName + "] 팀에 새로운 소식이 등록되었습니다!")
                .build();

        Message message = Message.builder()
                .setToken(token)
                .setNotification(notification)
                .build();

        return firebaseMessaging.send(message);
    }
}
