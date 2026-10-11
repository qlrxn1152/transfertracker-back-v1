package com.dhoon.transfertracker.etc.notice.controller;

import com.dhoon.transfertracker.etc.notice.dto.response.NoticesResponseDto;
import com.dhoon.transfertracker.etc.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping("/api/notices")
    public NoticesResponseDto getNotices() {
        return noticeService.getNotices();
    }
}
