package com.dhoon.transfertracker.etc.notification.domain;

import com.dhoon.transfertracker.internal.member.domain.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        indexes = @Index(name = "idx_member_push_subscription_member", columnList = "member_id"),
        uniqueConstraints = @UniqueConstraint(name = "uk_member_push_endpoint", columnNames = "endpoint")
)
public class MemberPushSubscription {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_push_subscription_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 1200)
    private String endpoint;

    @Column(nullable = false, length = 400)
    private String p256dh;

    @Column(nullable = false, length = 120)
    private String auth;

    @Column(name = "expiration_time")
    private Long expirationTime;

    @Column(name = "subscribed_at", nullable = false)
    private Instant subscribedAt;

    @Column(name = "last_failed_at")
    private Instant lastFailedAt;

    private MemberPushSubscription(
            Member member,
            String endpoint,
            String p256dh,
            String auth,
            Long expirationTime
    ) {
        this.member = member;
        this.endpoint = endpoint;
        this.p256dh = p256dh;
        this.auth = auth;
        this.expirationTime = expirationTime;
        this.subscribedAt = Instant.now();
    }

    public static MemberPushSubscription of(
            Member member,
            String endpoint,
            String p256dh,
            String auth,
            Long expirationTime
    ) {
        return new MemberPushSubscription(member, endpoint, p256dh, auth, expirationTime);
    }

    public void update(Member member, String p256dh, String auth, Long expirationTime) {
        this.member = member;
        this.p256dh = p256dh;
        this.auth = auth;
        this.expirationTime = expirationTime;
        this.subscribedAt = Instant.now();
        this.lastFailedAt = null;
    }

    public void markFailed() {
        this.lastFailedAt = Instant.now();
    }
}
