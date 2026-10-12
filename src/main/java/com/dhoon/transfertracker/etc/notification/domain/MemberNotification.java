package com.dhoon.transfertracker.etc.notification.domain;

import com.dhoon.transfertracker.internal.member.domain.Member;
import com.dhoon.transfertracker.internal.team.domain.Team;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        indexes = {
                @Index(name = "idx_member_notification_member_created", columnList = "member_id, created_at"),
                @Index(name = "idx_member_notification_member_read", columnList = "member_id, read_at")
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uk_member_notification_target",
                columnNames = {"member_id", "target_type", "target_id"}
        )
)
public class MemberNotification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_notification_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 40)
    private NotificationTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 600)
    private String message;

    @Column(name = "target_url", nullable = false, length = 200)
    private String targetUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    private MemberNotification(
            Member member,
            Team team,
            NotificationType type,
            NotificationTargetType targetType,
            Long targetId,
            String title,
            String message,
            String targetUrl
    ) {
        this.member = member;
        this.team = team;
        this.type = type;
        this.targetType = targetType;
        this.targetId = targetId;
        this.title = title;
        this.message = message;
        this.targetUrl = targetUrl;
        this.createdAt = Instant.now();
    }

    public static MemberNotification of(
            Member member,
            Team team,
            NotificationType type,
            NotificationTargetType targetType,
            Long targetId,
            String title,
            String message,
            String targetUrl
    ) {
        return new MemberNotification(member, team, type, targetType, targetId, title, message, targetUrl);
    }

    public boolean isRead() {
        return readAt != null;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void markRead() {
        if (readAt == null) {
            readAt = Instant.now();
        }
    }

    public void markDeleted() {
        if (deletedAt == null) {
            deletedAt = Instant.now();
        }
    }
}
