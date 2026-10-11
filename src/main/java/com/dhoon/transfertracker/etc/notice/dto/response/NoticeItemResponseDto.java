package com.dhoon.transfertracker.etc.notice.dto.response;

import com.dhoon.transfertracker.etc.notice.domain.Notice;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NoticeItemResponseDto {

    private Long noticeId;
    private String title;
    private String content;
    private LocalDateTime createdAt;

    public static NoticeItemResponseDto of(Notice notice) {
        return new NoticeItemResponseDto(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getCreatedAt()
        );
    }
}
