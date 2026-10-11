package com.dhoon.transfertracker.internal.admin.controller;

import com.dhoon.transfertracker.internal.analytics.dto.response.AdminAnalyticsResponseDto;
import com.dhoon.transfertracker.internal.analytics.service.AnalyticsService;
import com.dhoon.transfertracker.internal.admin.dto.response.*;
import com.dhoon.transfertracker.internal.notice.dto.request.NoticeCreateRequestDto;
import com.dhoon.transfertracker.internal.notice.dto.response.NoticeItemResponseDto;
import com.dhoon.transfertracker.internal.notice.service.NoticeService;
import com.dhoon.transfertracker.internal.admin.service.AdminService;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final NoticeService noticeService;
    private final AnalyticsService analyticsService;

    @GetMapping("/overview")
    public AdminOverviewResponseDto getOverview() {
        return adminService.getOverview();
    }

    @GetMapping("/data-quality")
    public AdminDataQualityResponseDto getDataQuality() {
        return adminService.getDataQuality();
    }

    @GetMapping("/db-summary")
    public AdminDbSummaryResponseDto getDbSummary() {
        return adminService.getDbSummary();
    }

    @GetMapping("/sources")
    public AdminSourcesResponseDto getSources() {
        return adminService.getSources();
    }

    @GetMapping("/analytics")
    public AdminAnalyticsResponseDto getAnalytics() {
        return analyticsService.getAdminAnalytics();
    }

    @GetMapping("/jobs")
    public AdminJobLogsResponseDto getJobLogs() {
        return adminService.getJobLogs();
    }

    @GetMapping("/sync/dry-run")
    public AdminDryRunResponseDto getDryRun() {
        return adminService.getDryRun();
    }

    @GetMapping("/diff-keys")
    public AdminDiffKeysResponseDto getDiffKeys() {
        return adminService.getDiffKeys();
    }

    @PostMapping("/sync/posts/{source}")
    public AdminSyncResponseDto syncPosts(@PathVariable TransferPostSource source) {
        return adminService.syncPosts(source);
    }

    @PostMapping("/sync/post-teams")
    public AdminSyncResponseDto assignPostTeams() {
        return adminService.assignPostTeams();
    }

    @PostMapping("/sync/translations")
    public AdminSyncResponseDto translatePosts() {
        return adminService.translatePosts();
    }

    @PostMapping("/sync/player-names")
    public AdminSyncResponseDto translatePlayerNames() {
        return adminService.translatePlayerNames();
    }

    @PostMapping("/notices")
    public NoticeItemResponseDto createNotice(@Valid @RequestBody NoticeCreateRequestDto request) {
        return noticeService.createNotice(request);
    }
}
