package com.dhoon.transfertracker.etc.analytics.service;

import com.dhoon.transfertracker.etc.analytics.domain.PageView;
import com.dhoon.transfertracker.etc.analytics.dto.request.PageViewCreateRequestDto;
import com.dhoon.transfertracker.etc.analytics.dto.response.AdminAnalyticsMetricResponseDto;
import com.dhoon.transfertracker.etc.analytics.dto.response.AdminAnalyticsResponseDto;
import com.dhoon.transfertracker.etc.analytics.dto.response.AdminPopularPageResponseDto;
import com.dhoon.transfertracker.etc.analytics.repository.PageViewRepository;
import com.dhoon.transfertracker.internal.member.repository.MemberFavoriteTeamRepository;
import com.dhoon.transfertracker.internal.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AnalyticsService {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    private final PageViewRepository pageViewRepository;
    private final MemberRepository memberRepository;
    private final MemberFavoriteTeamRepository memberFavoriteTeamRepository;

    public void recordPageView(PageViewCreateRequestDto request, String userAgent) {
        pageViewRepository.save(PageView.of(
                request.getVisitorId(),
                request.getSessionId(),
                request.getPath(),
                request.getPageTitle(),
                request.getReferrer(),
                userAgent
        ));
    }

    @Transactional(readOnly = true)
    public AdminAnalyticsResponseDto getAdminAnalytics() {
        LocalDateTime todayStart = LocalDate.now(KOREA_ZONE).atStartOfDay();
        LocalDateTime activeSince = LocalDateTime.now(KOREA_ZONE).minusMinutes(5);
        LocalDateTime weekStart = LocalDateTime.now(KOREA_ZONE).minusDays(7);

        long todayVisitors = pageViewRepository.countDistinctVisitorsByCreatedAtGreaterThanEqual(todayStart);
        long activeVisitors = pageViewRepository.countDistinctVisitorsByCreatedAtGreaterThanEqual(activeSince);
        long todayPageViews = pageViewRepository.countByCreatedAtGreaterThanEqual(todayStart);
        long totalMembers = memberRepository.count();
        long favoriteMembers = memberFavoriteTeamRepository.countDistinctMemberIds();

        List<AdminAnalyticsMetricResponseDto> metrics = List.of(
                AdminAnalyticsMetricResponseDto.of("오늘 방문자", todayVisitors, "오늘 한 번 이상 방문한 브라우저 기준입니다.", todayVisitors > 0 ? "good" : "neutral"),
                AdminAnalyticsMetricResponseDto.of("현재 접속 중", activeVisitors, "최근 5분 안에 페이지를 본 방문자입니다.", activeVisitors > 0 ? "good" : "neutral"),
                AdminAnalyticsMetricResponseDto.of("오늘 페이지뷰", todayPageViews, "오늘 기록된 페이지 이동 횟수입니다.", todayPageViews > 0 ? "good" : "neutral"),
                AdminAnalyticsMetricResponseDto.of("가입 회원", totalMembers, "회원가입을 완료한 계정 수입니다.", totalMembers > 0 ? "good" : "neutral"),
                AdminAnalyticsMetricResponseDto.of("관심팀 사용자", favoriteMembers, "관심팀을 하나 이상 등록한 회원 수입니다.", favoriteMembers > 0 ? "good" : "neutral")
        );

        List<AdminPopularPageResponseDto> popularPages = pageViewRepository
                .findPopularPagesByCreatedAtGreaterThanEqual(weekStart, PageRequest.of(0, 8))
                .stream()
                .map(row -> AdminPopularPageResponseDto.of((String) row[0], ((Number) row[1]).longValue()))
                .toList();

        return AdminAnalyticsResponseDto.of(metrics, popularPages);
    }
}
