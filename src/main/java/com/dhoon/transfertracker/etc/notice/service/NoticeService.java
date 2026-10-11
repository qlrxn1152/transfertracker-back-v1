package com.dhoon.transfertracker.etc.notice.service;

import com.dhoon.transfertracker.etc.notice.domain.Notice;
import com.dhoon.transfertracker.etc.notice.dto.request.NoticeCreateRequestDto;
import com.dhoon.transfertracker.etc.notice.dto.response.NoticeItemResponseDto;
import com.dhoon.transfertracker.etc.notice.dto.response.NoticesResponseDto;
import com.dhoon.transfertracker.etc.notice.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NoticeService {

    private final NoticeRepository noticeRepository;

    @Transactional(readOnly = true)
    public NoticesResponseDto getNotices() {
        return NoticesResponseDto.of(
                noticeRepository.findTop5ByActiveTrueOrderByCreatedAtDescIdDesc()
                        .stream()
                        .map(NoticeItemResponseDto::of)
                        .toList()
        );
    }

    public NoticeItemResponseDto createNotice(NoticeCreateRequestDto request) {
        Notice notice = noticeRepository.save(Notice.of(request.getTitle().trim(), request.getContent().trim()));

        return NoticeItemResponseDto.of(notice);
    }
}
