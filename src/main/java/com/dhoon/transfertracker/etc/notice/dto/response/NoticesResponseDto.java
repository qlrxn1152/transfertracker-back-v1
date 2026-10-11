package com.dhoon.transfertracker.etc.notice.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NoticesResponseDto {

    private List<NoticeItemResponseDto> notices;

    public static NoticesResponseDto of(List<NoticeItemResponseDto> notices) {
        return new NoticesResponseDto(notices);
    }
}
