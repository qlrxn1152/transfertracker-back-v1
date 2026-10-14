package com.dhoon.transfertracker.etc.admin.controller;

import com.dhoon.transfertracker.etc.admin.dto.request.AdminTransferPostImportRequestDto;
import com.dhoon.transfertracker.etc.admin.dto.response.*;
import com.dhoon.transfertracker.etc.analytics.dto.response.AdminAnalyticsResponseDto;
import com.dhoon.transfertracker.etc.analytics.service.AnalyticsService;
import com.dhoon.transfertracker.etc.notice.dto.request.NoticeCreateRequestDto;
import com.dhoon.transfertracker.etc.notice.dto.response.NoticeItemResponseDto;
import com.dhoon.transfertracker.etc.notice.service.NoticeService;
import com.dhoon.transfertracker.etc.admin.service.AdminService;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
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

    @PostMapping("/import/posts/{source}")
    public AdminSyncResponseDto importPosts(@PathVariable TransferPostSource source,
                                            @Valid @RequestBody AdminTransferPostImportRequestDto request) {
        return adminService.importPosts(source, request);
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
