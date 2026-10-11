package com.dhoon.transfertracker.etc.notification.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberNotificationsResponseDto {

    private long unreadCount;
    private int createdCount;
    private List<MemberNotificationItemResponseDto> notifications = new ArrayList<>();

    public static MemberNotificationsResponseDto of(
            long unreadCount,
            int createdCount,
            List<MemberNotificationItemResponseDto> notifications
    ) {
        return new MemberNotificationsResponseDto(unreadCount, createdCount, notifications);
    }
}
