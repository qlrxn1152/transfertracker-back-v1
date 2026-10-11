package com.dhoon.transfertracker.etc.notification.service;

import com.dhoon.transfertracker.etc.notification.config.WebPushProperties;
import com.dhoon.transfertracker.etc.notification.domain.MemberNotification;
import com.dhoon.transfertracker.etc.notification.domain.MemberPushSubscription;
import com.dhoon.transfertracker.etc.notification.dto.request.MemberPushSubscriptionRequestDto;
import com.dhoon.transfertracker.etc.notification.dto.response.PushConfigResponseDto;
import com.dhoon.transfertracker.etc.notification.repository.MemberPushSubscriptionRepository;
import com.dhoon.transfertracker.internal.member.domain.Member;
import com.dhoon.transfertracker.internal.member.exception.NotFoundMemberException;
import com.dhoon.transfertracker.internal.member.repository.MemberRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Security;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MemberPushNotificationService {

    private final WebPushProperties webPushProperties;
    private final MemberRepository memberRepository;
    private final MemberPushSubscriptionRepository memberPushSubscriptionRepository;

    @PostConstruct
    public void registerBouncyCastleProvider() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public PushConfigResponseDto getConfig() {
        return PushConfigResponseDto.of(webPushProperties.enabled(), webPushProperties.publicKey());
    }

    public void subscribe(Long memberId, MemberPushSubscriptionRequestDto request) {
        validateEnabled();

        Member member = memberRepository.findById(memberId)
                .orElseThrow(NotFoundMemberException::new);

        MemberPushSubscription subscription = memberPushSubscriptionRepository.findByEndpoint(request.getEndpoint())
                .orElse(null);

        if (subscription == null) {
            memberPushSubscriptionRepository.save(MemberPushSubscription.of(
                    member,
                    request.getEndpoint(),
                    request.getP256dh(),
                    request.getAuth(),
                    request.getExpirationTime()
            ));
            return;
        }

        subscription.update(member, request.getP256dh(), request.getAuth(), request.getExpirationTime());
    }

    public void unsubscribe(Long memberId, MemberPushSubscriptionRequestDto request) {
        memberPushSubscriptionRepository.deleteByMemberIdAndEndpoint(memberId, request.getEndpoint());
    }

    public void send(MemberNotification notification) {
        if (!webPushProperties.enabled()) {
            return;
        }

        List<MemberPushSubscription> subscriptions = memberPushSubscriptionRepository.findAllByMemberId(
                notification.getMember().getId()
        );

        if (subscriptions.isEmpty()) {
            return;
        }

        String payload = toPayload(notification);

        for (MemberPushSubscription subscription : subscriptions) {
            send(subscription, payload);
        }
    }

    private void send(MemberPushSubscription subscription, String payload) {
        try {
            PushService pushService = new PushService(
                    webPushProperties.publicKey(),
                    webPushProperties.privateKey(),
                    webPushProperties.subjectOrDefault()
            );
            Subscription webPushSubscription = new Subscription(
                    subscription.getEndpoint(),
                    new Subscription.Keys(subscription.getP256dh(), subscription.getAuth())
            );
            HttpResponse response = pushService.send(new Notification(webPushSubscription, payload));
            int statusCode = response.getStatusLine().getStatusCode();
            EntityUtils.consumeQuietly(response.getEntity());

            if (statusCode == 404 || statusCode == 410) {
                memberPushSubscriptionRepository.delete(subscription);
                return;
            }

            if (statusCode >= 400) {
                subscription.markFailed();
                log.warn("Web push failed. status={}, endpoint={}", statusCode, subscription.getEndpoint());
            }
        } catch (Exception exception) {
            subscription.markFailed();
            log.warn("Web push failed. endpoint={}", subscription.getEndpoint(), exception);
        }
    }

    private String toPayload(MemberNotification notification) {
        return "{"
                + "\"title\":\"" + escapeJson(notification.getTitle()) + "\","
                + "\"body\":\"" + escapeJson(notification.getMessage()) + "\","
                + "\"url\":\"" + escapeJson(notification.getTargetUrl()) + "\","
                + "\"notificationId\":" + notification.getId()
                + "}";
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private void validateEnabled() {
        if (!webPushProperties.enabled()) {
            throw new IllegalStateException("푸시 알림 설정이 아직 완료되지 않았습니다.");
        }
    }
}
