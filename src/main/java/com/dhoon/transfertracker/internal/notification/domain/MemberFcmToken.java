package com.dhoon.transfertracker.internal.notification.domain;

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
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_member_fcm_token_token", columnNames = "token")
        }
)
public class MemberFcmToken {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_fcm_token_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FcmPlatform platform;

    @Column(nullable = false, length = 512)
    private String token;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static MemberFcmToken of(Member member, FcmPlatform platform, String token) {
        return new MemberFcmToken(member, platform, token);
    }

    public void changeMemberFcmToken(Member member, FcmPlatform platform) {
        this.member = member;
        this.platform = platform;
    }

    private MemberFcmToken(Member member, FcmPlatform platform, String token) {
        this.member = member;
        this.platform = platform;
        this.token = token;
        this.createdAt = Instant.now();
    }

}
