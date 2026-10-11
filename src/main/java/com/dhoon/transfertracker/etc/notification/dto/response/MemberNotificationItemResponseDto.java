package com.dhoon.transfertracker.etc.notification.dto.response;

import com.dhoon.transfertracker.etc.notification.domain.MemberNotification;
import com.dhoon.transfertracker.etc.notification.domain.NotificationTargetType;
import com.dhoon.transfertracker.etc.notification.domain.NotificationType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberNotificationItemResponseDto {

    private Long notificationId;
    private NotificationType type;
    private NotificationTargetType targetType;
    private Long targetId;
    private Long teamId;
    private String teamName;
    private String title;
    private String message;
    private String targetUrl;
    private boolean read;
    private Instant createdAt;

    public static MemberNotificationItemResponseDto of(MemberNotification notification) {
        return new MemberNotificationItemResponseDto(
                notification.getId(),
                notification.getType(),
                notification.getTargetType(),
                notification.getTargetId(),
                notification.getTeam() == null ? null : notification.getTeam().getId(),
                notification.getTeam() == null ? null : notification.getTeam().getDisplayName(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getTargetUrl(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
