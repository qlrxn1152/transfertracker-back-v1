package com.dhoon.transfertracker.internal.admin.service;

import com.dhoon.transfertracker.external.x.client.XClient;
import com.dhoon.transfertracker.internal.admin.domain.AdminJobLog;
import com.dhoon.transfertracker.internal.admin.dto.response.*;
import com.dhoon.transfertracker.internal.admin.repository.AdminJobLogRepository;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.transfer.domain.Transfer;
import com.dhoon.transfertracker.internal.transfer.repository.TransferRepository;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPost;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.transferpost.domain.TranslateStatus;
import com.dhoon.transfertracker.internal.transferpost.repository.TransferPostRepository;
import com.dhoon.transfertracker.internal.transferpost.service.impl.TransferPostOrchestrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final TransferRepository transferRepository;
    private final TransferPostRepository transferPostRepository;
    private final AdminJobLogRepository adminJobLogRepository;
    private final TransferPostOrchestrationService transferPostOrchestrationService;
    private final XClient xClient;
    private final Environment environment;

    @Transactional(readOnly = true)
    public AdminOverviewResponseDto getOverview() {
        long todayPosts = countTodayPosts();
        long pendingTranslations = transferPostRepository.countByTranslateStatus(TranslateStatus.PENDING);
        long failedTranslations = transferPostRepository.countByTranslateStatus(TranslateStatus.FAILED);
        long unassignedPosts = transferPostRepository.countByTeamIsNull();

        List<AdminMetricResponseDto> metrics = List.of(
                AdminMetricResponseDto.of("오늘 수집 게시물", todayPosts, "정상", "good", "오늘 수집된 기자 게시물 수입니다."),
                AdminMetricResponseDto.of("번역 대기", pendingTranslations, "처리 필요", pendingTranslations > 0 ? "warn" : "good", "아직 한국어 번역이 완료되지 않은 게시물입니다."),
                AdminMetricResponseDto.of("팀 미연결 게시물", unassignedPosts, "확인 필요", unassignedPosts > 0 ? "bad" : "good", "팀과 연결되지 않아 화면 필터링에서 빠질 수 있는 게시물입니다."),
                AdminMetricResponseDto.of("번역 실패", failedTranslations, "재시도", failedTranslations > 0 ? "bad" : "good", "외부 번역 API 처리에 실패한 게시물입니다.")
        );

        List<AdminTaskResponseDto> tasks = List.of(
                AdminTaskResponseDto.of("운영/로컬 데이터 차이 확인", "운영 DB와 로컬 DB의 테이블별 건수를 비교합니다.", "검수", "warn"),
                AdminTaskResponseDto.of("팀 미연결 게시물 처리", "게시물 본문 기준으로 팀을 다시 매칭합니다.", "재매칭", unassignedPosts > 0 ? "bad" : "good"),
                AdminTaskResponseDto.of("번역 실패 재시도", "FAILED 상태의 게시물을 확인하고 다시 번역합니다.", "재시도", failedTranslations > 0 ? "bad" : "good")
        );

        return AdminOverviewResponseDto.of(metrics, tasks);
    }

    @Transactional(readOnly = true)
    public AdminDbSummaryResponseDto getDbSummary() {
        List<AdminDbSummaryItemResponseDto> tables = List.of(
                AdminDbSummaryItemResponseDto.of("teams", teamRepository.count(), "팀 기준 데이터"),
                AdminDbSummaryItemResponseDto.of("players", playerRepository.count(), "선수 기준 데이터"),
                AdminDbSummaryItemResponseDto.of("transfers", transferRepository.count(), "이적 기록"),
                AdminDbSummaryItemResponseDto.of("transfer_posts", transferPostRepository.count(), "기자 게시물")
        );

        return AdminDbSummaryResponseDto.of(tables);
    }

    @Transactional(readOnly = true)
    public AdminDataQualityResponseDto getDataQuality() {
        long pendingTranslations = transferPostRepository.countByTranslateStatus(TranslateStatus.PENDING);
        long failedTranslations = transferPostRepository.countByTranslateStatus(TranslateStatus.FAILED);
        long unassignedPosts = transferPostRepository.countByTeamIsNull();

        List<AdminDataIssueResponseDto> issues = List.of(
                AdminDataIssueResponseDto.of("게시물", "team_id", "팀과 연결되지 않은 게시물 " + unassignedPosts + "건", "팀 재매칭", unassignedPosts > 0 ? "bad" : "good"),
                AdminDataIssueResponseDto.of("번역", "PENDING", "번역 대기 게시물 " + pendingTranslations + "건", "번역 실행", pendingTranslations > 0 ? "warn" : "good"),
                AdminDataIssueResponseDto.of("번역", "FAILED", "번역 실패 게시물 " + failedTranslations + "건", "실패 확인", failedTranslations > 0 ? "bad" : "good"),
                AdminDataIssueResponseDto.of("운영/로컬", "db-summary", "환경별 DB 요약을 비교할 수 있도록 현재 환경의 테이블 건수를 제공합니다.", "스냅샷 비교", "neutral")
        );

        return AdminDataQualityResponseDto.of(issues);
    }

    @Transactional(readOnly = true)
    public AdminSourcesResponseDto getSources() {
        List<AdminSourceResponseDto> sources = Arrays.stream(TransferPostSource.values())
                .map(source -> {
                    long posts = transferPostRepository.countBySourcer(source);
                    String status = posts > 0 ? "정상" : "수집 필요";
                    String tone = posts > 0 ? "good" : "warn";

                    return AdminSourceResponseDto.of(source.name(), source.getXUsername(), posts, status, tone);
                })
                .toList();

        return AdminSourcesResponseDto.of(sources);
    }

    @Transactional(readOnly = true)
    public AdminJobLogsResponseDto getJobLogs() {
        List<AdminJobLogResponseDto> logs = adminJobLogRepository.findTop30ByOrderByCreatedAtDescIdDesc()
                .stream()
                .map(AdminJobLogResponseDto::of)
                .toList();

        return AdminJobLogsResponseDto.of(logs);
    }

    @Transactional(readOnly = true)
    public AdminDryRunResponseDto getDryRun() {
        long unassignedPosts = transferPostRepository.countByTeamIsNull();
        long matchablePosts = countMatchableUnassignedPosts();
        long pendingTranslations = transferPostRepository.countByTranslateStatus(TranslateStatus.PENDING);

        List<AdminDryRunJobResponseDto> jobs = new ArrayList<>();
        Arrays.stream(TransferPostSource.values())
                .forEach(source -> {
                    long currentPosts = transferPostRepository.countBySourcer(source);
                    jobs.add(AdminDryRunJobResponseDto.of(
                            "sync-posts:" + source.name(),
                            source.getXUsername() + " 게시물 수집",
                            currentPosts,
                            "현재 저장된 게시물은 " + currentPosts + "건입니다. 외부 X 조회는 실제 실행에서만 수행합니다.",
                            "neutral"
                    ));
                });

        jobs.add(AdminDryRunJobResponseDto.of(
                "assign-post-teams",
                "게시물 팀 재매칭",
                matchablePosts,
                "팀 미연결 게시물 " + unassignedPosts + "건 중 " + matchablePosts + "건은 현재 팀 이름 기준으로 매칭 가능해 보입니다.",
                matchablePosts > 0 ? "warn" : "good"
        ));
        jobs.add(AdminDryRunJobResponseDto.of(
                "translate-posts",
                "게시물 번역",
                pendingTranslations,
                "번역 대기 게시물 " + pendingTranslations + "건이 처리 대상입니다.",
                pendingTranslations > 0 ? "warn" : "good"
        ));

        return AdminDryRunResponseDto.of(jobs);
    }

    @Transactional(readOnly = true)
    public AdminDiffKeysResponseDto getDiffKeys() {
        List<AdminDiffKeyItemResponseDto> items = new ArrayList<>();

        teamRepository.findAll().forEach(team -> items.add(AdminDiffKeyItemResponseDto.of(
                "team",
                "teams",
                team.getId(),
                String.valueOf(team.getApiFootballId()),
                team.getTeamName()
        )));

        playerRepository.findAll().forEach(player -> items.add(AdminDiffKeyItemResponseDto.of(
                "player",
                "players",
                player.getId(),
                String.valueOf(player.getApiFootballId()),
                player.getPlayerName()
        )));

        transferPostRepository.findAll().forEach(post -> items.add(AdminDiffKeyItemResponseDto.of(
                "transfer_post",
                "transfer_posts",
                post.getId(),
                post.getExternalPostId(),
                trimLabel(post.getContent())
        )));

        transferRepository.findAllWithLazyEntities().forEach(transfer -> items.add(AdminDiffKeyItemResponseDto.of(
                "transfer",
                "transfers",
                transfer.getId(),
                transferKey(transfer),
                transferLabel(transfer)
        )));

        return AdminDiffKeysResponseDto.of(items);
    }

    public AdminSyncResponseDto syncPosts(TransferPostSource source) {
        String job = "sync-posts:" + source.name();
        long beforeCount = transferPostRepository.countBySourcer(source);

        try {
            xClient.syncPosts(source);
            long afterCount = transferPostRepository.countBySourcer(source);
            long affectedCount = Math.max(0, afterCount - beforeCount);

            return completeJob(
                    job,
                    source.getXUsername() + " 게시물 수집을 완료했습니다.",
                    affectedCount,
                    "before=" + beforeCount + ", after=" + afterCount
            );
        } catch (RuntimeException exception) {
            failJob(job, exception);
            throw exception;
        }
    }

    public AdminSyncResponseDto assignPostTeams() {
        String job = "assign-post-teams";
        long beforeCount = transferPostRepository.countByTeamIsNull();

        try {
            transferPostOrchestrationService.postAssignTeam();
            long afterCount = transferPostRepository.countByTeamIsNull();
            long affectedCount = Math.max(0, beforeCount - afterCount);

            return completeJob(
                    job,
                    "게시물 팀 재매칭을 완료했습니다.",
                    affectedCount,
                    "team_id null before=" + beforeCount + ", after=" + afterCount
            );
        } catch (RuntimeException exception) {
            failJob(job, exception);
            throw exception;
        }
    }

    public AdminSyncResponseDto translatePosts() {
        String job = "translate-posts";
        long beforeCount = transferPostRepository.countByTranslateStatus(TranslateStatus.PENDING);

        try {
            transferPostOrchestrationService.translate();
            long afterCount = transferPostRepository.countByTranslateStatus(TranslateStatus.PENDING);
            long affectedCount = Math.max(0, beforeCount - afterCount);

            return completeJob(
                    job,
                    "번역 대기 게시물 처리를 완료했습니다.",
                    affectedCount,
                    "pending before=" + beforeCount + ", after=" + afterCount
            );
        } catch (RuntimeException exception) {
            failJob(job, exception);
            throw exception;
        }
    }

    private long countTodayPosts() {
        LocalDate today = LocalDate.now(KOREA_ZONE);
        Instant start = today.atStartOfDay(KOREA_ZONE).toInstant();
        Instant end = today.plusDays(1).atStartOfDay(KOREA_ZONE).toInstant();

        return transferPostRepository.countByContentCreatedAtBetween(start, end);
    }

    private long countMatchableUnassignedPosts() {
        List<TransferPost> unassignedPosts = transferPostRepository.findAllByTeamIsNull();
        List<Team> teams = teamRepository.findAllByLeagueCodeIsNotNull();

        return unassignedPosts.stream()
                .filter(post -> {
                    String content = post.getContent().toLowerCase();
                    return teams.stream()
                            .map(Team::getTeamName)
                            .map(String::toLowerCase)
                            .anyMatch(content::contains);
                })
                .count();
    }

    private AdminSyncResponseDto completeJob(String job, String message, long affectedCount, String detail) {
        adminJobLogRepository.save(AdminJobLog.of(job, "COMPLETED", message, affectedCount, detail, resolveEnvironmentName()));
        return AdminSyncResponseDto.of(job, "COMPLETED", message);
    }

    private void failJob(String job, RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }

        adminJobLogRepository.save(AdminJobLog.of(job, "FAILED", message, 0, exception.getClass().getName(), resolveEnvironmentName()));
    }

    private String resolveEnvironmentName() {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length == 0) {
            return "local";
        }

        return String.join(",", activeProfiles);
    }

    private static String transferKey(Transfer transfer) {
        return transfer.getPlayer().getApiFootballId()
                + "|" + transfer.getInTeam().getApiFootballId()
                + "|" + transfer.getOutTeam().getApiFootballId()
                + "|" + transfer.getTransferDate();
    }

    private static String transferLabel(Transfer transfer) {
        return transfer.getPlayer().getPlayerName()
                + " / " + transfer.getOutTeam().getTeamName()
                + " -> " + transfer.getInTeam().getTeamName()
                + " / " + transfer.getTransferDate();
    }

    private static String trimLabel(String label) {
        if (label == null) {
            return "";
        }

        if (label.length() <= 120) {
            return label;
        }

        return label.substring(0, 120);
    }
}
