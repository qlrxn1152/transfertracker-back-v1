package com.dhoon.transfertracker.internal.team.sync.service.impl;

import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.TeamCollectionLeague;
import com.dhoon.transfertracker.internal.team.sync.dto.FootballDataSyncProgress;
import com.dhoon.transfertracker.internal.team.sync.dto.record.FootballDataSyncResponse;
import com.dhoon.transfertracker.internal.team.sync.dto.record.TeamCollectionTarget;
import com.dhoon.transfertracker.internal.teamplayer.sync.service.impl.TeamPlayerSyncOrchestrationService;
import com.dhoon.transfertracker.internal.transfer.sync.dto.response.TeamTransfersSaveResponseDto;
import com.dhoon.transfertracker.internal.transfer.sync.service.impl.TransferSyncOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class FootballDataSyncOrchestrationService {

    private static final int WORKER_COUNT = 8;

    private final TeamCollectionTargetService targetService;
    private final TeamSyncTxService teamSyncTxService;

    private final TeamPlayerSyncOrchestrationService
            teamPlayerSyncOrchestrationService;

    private final TransferSyncOrchestrationService
            transferSyncOrchestrationService;

    // 통합 동기화 작업 내부의 DB 저장 구간 보호
    private final ReentrantLock databaseLock = new ReentrantLock(true);

    private volatile SyncJob currentJob;


    // ============================================
    // Public API
    // ============================================

    public FootballDataSyncResponse syncAll() {

        return syncFootballData(
                getAllTargetLeagueIds(),
                true,
                true
        );
    }

    public FootballDataSyncResponse syncTeams() {

        return syncFootballData(
                getAllTargetLeagueIds(),
                false,
                false
        );
    }

    public FootballDataSyncResponse syncPlayers() {

        return syncFootballData(
                getAllTargetLeagueIds(),
                true,
                false
        );
    }

    public FootballDataSyncResponse syncTransfers() {

        return syncFootballData(
                getAllTargetLeagueIds(),
                false,
                true
        );
    }

    public FootballDataSyncResponse syncLeague(Long leagueApiId) {

        if (!getAllTargetLeagueIds().contains(leagueApiId)) {

            throw new IllegalArgumentException(
                    "지원하지 않는 수집 대상 리그입니다."
            );
        }

        return syncFootballData(
                Set.of(leagueApiId),
                true,
                true
        );
    }


    // ============================================
    // 전체 동기화
    // ============================================

    private FootballDataSyncResponse syncFootballData(
            Set<Long> leagueApiIds,
            boolean syncPlayers,
            boolean syncTransfers
    ) {

        SyncJob job = startJob();

        ExecutorService executorService = null;

        try {

            Map<Long, TeamCollectionTarget> targets =
                    collectTargets(leagueApiIds, job);

            job.totalTeamCount = targets.size();

            if (job.stopRequested) {

                job.finish("CANCELLED");

                return createResponse(leagueApiIds.size(), job);
            }

            job.status = "RUNNING";

            executorService = Executors.newFixedThreadPool(
                    WORKER_COUNT
            );

            // 모든 수집 대상 팀을 공용 작업 큐에 등록
            Queue<TeamCollectionTarget> taskQueue =
                    new ConcurrentLinkedQueue<>(
                            targets.values()
                    );

            List<CompletableFuture<Void>> workers =
                    new ArrayList<>();

            for (int i = 0; i < WORKER_COUNT; i++) {

                CompletableFuture<Void> worker =
                        CompletableFuture.runAsync(
                                () -> executeWorker(
                                        taskQueue,
                                        syncPlayers,
                                        syncTransfers,
                                        job
                                ),
                                executorService
                        );

                workers.add(worker);
            }

            // 모든 Worker가 종료될 때까지 대기
            CompletableFuture.allOf(
                    workers.toArray(CompletableFuture[]::new)
            ).join();

            job.finish(
                    job.stopRequested
                            ? "CANCELLED"
                            : "COMPLETED"
            );

            return createResponse(
                    leagueApiIds.size(),
                    job
            );

        } catch (RuntimeException e) {

            job.finish("FAILED");

            throw e;

        } finally {

            if (executorService != null) {

                executorService.shutdown();
            }
        }
    }


    // ============================================
    // Worker Thread
    // ============================================

    private void executeWorker(
            Queue<TeamCollectionTarget> taskQueue,
            boolean syncPlayers,
            boolean syncTransfers,
            SyncJob job
    ) {

        while (!job.stopRequested) {

            TeamCollectionTarget target = taskQueue.poll();

            if (target == null) {
                return;
            }

            boolean success = false;

            try {

                success = syncSingleTeam(
                        target,
                        syncPlayers,
                        syncTransfers,
                        job
                );

            } catch (Exception e) {

                addFailure(
                        job,
                        null,
                        target.teamApiId(),
                        "UNKNOWN",
                        e
                );

            } finally {

                if (!success) {
                    job.failedTeamCount.incrementAndGet();
                }

                int processed =
                        job.processedTeamCount.incrementAndGet();

                log.info(
                        "동기화 진행률 {}/{}, team={}, thread={}, success={}",
                        processed,
                        job.totalTeamCount,
                        target.teamName(),
                        Thread.currentThread().getName(),
                        success
                );
            }
        }
    }


    // ============================================
    // 팀 단위 동기화
    // ============================================

    private boolean syncSingleTeam(
            TeamCollectionTarget target,
            boolean syncPlayers,
            boolean syncTransfers,
            SyncJob job
    ) {

        Long teamApiId = target.teamApiId();

        boolean success = true;


        // STEP 1. 팀 저장
        try {

            databaseLock.lock();

            try {

                teamSyncTxService.syncCollectionTeam(
                        target.teamData(),
                        target.domesticLeague()
                );

                job.successTeamCount.incrementAndGet();

            } finally {

                databaseLock.unlock();
            }

        } catch (Exception e) {

            addFailure(
                    job,
                    null,
                    teamApiId,
                    "TEAM",
                    e
            );

            return false;
        }


        // STEP 2. 선수단 수집
        if (syncPlayers) {

            try {

                // 외부 API 호출은 Lock 밖에서 실행
                JsonNode squadResponse =
                        teamPlayerSyncOrchestrationService
                                .fetchSquad(teamApiId);

                // DB 저장 구간만 Lock 적용
                databaseLock.lock();

                try {

                    int playerCount;

                    if (target.national()) {

                        playerCount =
                                teamPlayerSyncOrchestrationService
                                        .saveNationalTeamPlayers(
                                                squadResponse
                                        );

                    } else {

                        playerCount =
                                teamPlayerSyncOrchestrationService
                                        .saveTeamPlayers(
                                                squadResponse
                                        )
                                        .getPlayers()
                                        .size();
                    }

                    job.processedSquadPlayerCount
                            .addAndGet(playerCount);

                    job.successSquadCount.incrementAndGet();

                } finally {

                    databaseLock.unlock();
                }

            } catch (Exception e) {

                success = false;

                addFailure(
                        job,
                        null,
                        teamApiId,
                        "SQUAD",
                        e
                );
            }
        }


        // STEP 3. 클럽팀 이적 기록 수집
        if (syncTransfers && !target.national()) {

            try {

                // 외부 API 호출은 병렬 실행
                JsonNode transferResponse =
                        transferSyncOrchestrationService
                                .fetchTeamTransfers(teamApiId);

                databaseLock.lock();

                try {

                    TeamTransfersSaveResponseDto response =
                            transferSyncOrchestrationService
                                    .saveTeamTransfers(
                                            teamApiId,
                                            transferResponse
                                    );

                    job.successTransferPlayerCount.addAndGet(
                            response.getSuccessPlayerCount()
                    );

                    job.failedTransferPlayerCount.addAndGet(
                            response.getFailedPlayerCount()
                    );

                    if (response.getFailedPlayerCount() == 0) {

                        job.successTransferTeamCount
                                .incrementAndGet();

                    } else {

                        success = false;

                        job.failures.add(
                                new FootballDataSyncResponse.Failure(
                                        null,
                                        teamApiId,
                                        "TRANSFER_PARTIAL",
                                        "실패 선수 수: "
                                                + response.getFailedPlayerCount()
                                                + ", 실패 선수 ID: "
                                                + response.getFailedPlayerIds()
                                )
                        );
                    }

                } finally {

                    databaseLock.unlock();
                }

            } catch (Exception e) {

                success = false;

                addFailure(
                        job,
                        null,
                        teamApiId,
                        "TRANSFER",
                        e
                );
            }
        }

        return success;
    }


    // ============================================
    // 수집 대상 조회
    // ============================================

    private Map<Long, TeamCollectionTarget> collectTargets(
            Set<Long> leagueApiIds,
            SyncJob job
    ) {

        Map<Long, TeamCollectionTarget> targets =
                new LinkedHashMap<>();

        for (Long leagueApiId : leagueApiIds) {

            if (job.stopRequested) {
                break;
            }

            try {

                List<TeamCollectionTarget> leagueTargets =
                        targetService.getCollectionTargets(
                                leagueApiId
                        );

                for (TeamCollectionTarget target : leagueTargets) {

                    targets.merge(
                            target.teamApiId(),
                            target,
                            this::mergeTarget
                    );
                }

            } catch (Exception e) {

                addFailure(
                        job,
                        leagueApiId,
                        null,
                        "COLLECT_TARGET",
                        e
                );
            }
        }

        return targets;
    }

    private TeamCollectionTarget mergeTarget(
            TeamCollectionTarget existing,
            TeamCollectionTarget incoming
    ) {

        if (existing.national() != incoming.national()) {

            throw new IllegalStateException(
                    "동일 팀의 국가대표 여부가 일치하지 않습니다."
            );
        }

        if (existing.domesticLeague() == null
                && incoming.domesticLeague() != null) {

            return incoming;
        }

        return existing;
    }

    private Set<Long> getAllTargetLeagueIds() {

        Set<Long> leagueIds = new LinkedHashSet<>();

        for (TeamCollectionLeague league
                : TeamCollectionLeague.values()) {

            leagueIds.add(league.getApiFootballLeagueId());
        }

        for (LeagueCode leagueCode : LeagueCode.values()) {

            leagueIds.add(leagueCode.getApiFootballLeagueId());
        }

        return leagueIds;
    }


    // ============================================
    // 작업 시작 및 중복 실행 방지
    // ============================================

    private synchronized SyncJob startJob() {

        if (currentJob != null && currentJob.isRunning()) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 축구 데이터 동기화가 진행 중입니다."
            );
        }

        currentJob = new SyncJob();

        return currentJob;
    }


    // ============================================
    // 진행률 조회
    // ============================================

    public FootballDataSyncProgress getProgress() {

        SyncJob job = currentJob;

        if (job == null) {

            return new FootballDataSyncProgress(
                    "IDLE",
                    0, 0, 0,
                    0, 0, 0,
                    0.0,
                    null,
                    null
            );
        }

        int total = job.totalTeamCount;
        int processed = job.processedTeamCount.get();

        double percent = total == 0
                ? 0.0
                : Math.round(
                processed * 10000.0 / total
        ) / 100.0;

        return new FootballDataSyncProgress(
                job.status,
                total,
                processed,
                job.failedTeamCount.get(),
                job.successTeamCount.get(),
                job.successSquadCount.get(),
                job.successTransferTeamCount.get(),
                percent,
                job.startedAt,
                job.finishedAt
        );
    }


    // ============================================
    // 작업 중단
    // ============================================

    public synchronized void stopSync() {

        SyncJob job = currentJob;

        if (job == null || !job.isRunning()) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "현재 진행 중인 동기화가 없습니다."
            );
        }

        job.requestStop();

        log.warn("축구 데이터 동기화 중단 요청");
    }


    // ============================================
    // 결과 생성 및 오류 처리
    // ============================================

    private FootballDataSyncResponse createResponse(
            int targetLeagueCount,
            SyncJob job
    ) {

        return new FootballDataSyncResponse(
                targetLeagueCount,
                job.totalTeamCount,
                job.successTeamCount.get(),
                job.successSquadCount.get(),
                job.successTransferTeamCount.get(),
                job.processedSquadPlayerCount.get(),
                job.successTransferPlayerCount.get(),
                job.failedTransferPlayerCount.get(),
                List.copyOf(job.failures)
        );
    }

    private void addFailure(
            SyncJob job,
            Long leagueApiId,
            Long teamApiId,
            String stage,
            Exception e
    ) {

        job.failures.add(
                new FootballDataSyncResponse.Failure(
                        leagueApiId,
                        teamApiId,
                        stage,
                        e.getMessage()
                )
        );

        log.error(
                "축구 데이터 동기화 실패. leagueApiId={}, teamApiId={}, stage={}",
                leagueApiId,
                teamApiId,
                stage,
                e
        );
    }


    // ============================================
    // 동기화 작업 상태
    // ============================================

    private static class SyncJob {

        private volatile String status = "COLLECTING";

        private volatile boolean stopRequested = false;

        private volatile int totalTeamCount;

        private final Instant startedAt = Instant.now();

        private volatile Instant finishedAt;

        private final AtomicInteger processedTeamCount =
                new AtomicInteger();

        private final AtomicInteger failedTeamCount =
                new AtomicInteger();

        private final AtomicInteger successTeamCount =
                new AtomicInteger();

        private final AtomicInteger successSquadCount =
                new AtomicInteger();

        private final AtomicInteger successTransferTeamCount =
                new AtomicInteger();

        private final AtomicInteger processedSquadPlayerCount =
                new AtomicInteger();

        private final AtomicInteger successTransferPlayerCount =
                new AtomicInteger();

        private final AtomicInteger failedTransferPlayerCount =
                new AtomicInteger();

        private final Queue<FootballDataSyncResponse.Failure>
                failures = new ConcurrentLinkedQueue<>();

        private boolean isRunning() {

            return finishedAt == null;
        }

        private synchronized void requestStop() {

            if (finishedAt == null) {

                stopRequested = true;
                status = "STOPPING";
            }
        }

        private synchronized void finish(String status) {

            this.status = this.stopRequested
                    && !"FAILED".equals(status)
                    ? "CANCELLED"
                    : status;

            this.finishedAt = Instant.now();
        }
    }
}