package com.dhoon.transfertracker.monitoring;

import com.dhoon.transfertracker.internal.post.domain.PostSyncStatusType;
import com.dhoon.transfertracker.internal.post.domain.PostSyncStep;
import com.dhoon.transfertracker.internal.post.repository.PostSyncStatusRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Gauge 조회 시점에 DB를 호출하지 않도록 상태 집계를 메모리에 캐시한다.
 * refresh_success와 last_success_epoch_seconds를 함께 내보내서 "실제 0건"과
 * "집계 실패/아직 미집계"를 Grafana에서 구분할 수 있다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostSyncStatusMetrics {

    private final MeterRegistry meterRegistry;
    private final PostSyncStatusRepository postSyncStatusRepository;

    @Value("${monitoring.post-sync-status.stale-after-minutes:15}")
    private long staleAfterMinutes;

    private final Map<PostSyncStep, Map<PostSyncStatusType, AtomicLong>> statusCounts = new EnumMap<>(PostSyncStep.class);
    private final Map<PostSyncStep, AtomicLong> staleCounts = new EnumMap<>(PostSyncStep.class);
    private final AtomicLong refreshSuccess = new AtomicLong();
    private final AtomicLong lastSuccessEpochSeconds = new AtomicLong();

    @PostConstruct
    void registerGauges() {
        for (PostSyncStep step : PostSyncStep.values()) {
            Map<PostSyncStatusType, AtomicLong> byStatus = new EnumMap<>(PostSyncStatusType.class);
            statusCounts.put(step, byStatus);
            for (PostSyncStatusType status : PostSyncStatusType.values()) {
                AtomicLong count = new AtomicLong();
                byStatus.put(status, count);
                Gauge.builder("veyro.post.sync.status", count, AtomicLong::get)
                        .description("Cached post synchronization status count")
                        .tags("step", step.name().toLowerCase(), "status", status.name().toLowerCase())
                        .register(meterRegistry);
            }
            AtomicLong stale = new AtomicLong();
            staleCounts.put(step, stale);
            Gauge.builder("veyro.post.sync.stale", stale, AtomicLong::get)
                    .description("Posts staying in processing longer than the configured threshold")
                    .tag("step", step.name().toLowerCase())
                    .register(meterRegistry);
        }
        Gauge.builder("veyro.post.sync.status.refresh.success", refreshSuccess, AtomicLong::get).register(meterRegistry);
        Gauge.builder("veyro.post.sync.status.last_success_epoch_seconds", lastSuccessEpochSeconds, AtomicLong::get).register(meterRegistry);
        refresh();
    }

    @Scheduled(fixedDelayString = "${monitoring.post-sync-status.refresh-interval-ms:30000}")
    @Transactional(readOnly = true)
    public void refresh() {
        try {
            LocalDateTime staleBefore = LocalDateTime.now().minusMinutes(staleAfterMinutes);
            for (PostSyncStep step : PostSyncStep.values()) {
                for (PostSyncStatusType status : PostSyncStatusType.values()) {
                    statusCounts.get(step).get(status).set(
                            postSyncStatusRepository.countByCurrentStepAndCurrentStepStatus(step, status)
                    );
                }
                staleCounts.get(step).set(postSyncStatusRepository
                        .countByCurrentStepAndCurrentStepStatusAndUpdatedAtBefore(
                                step, PostSyncStatusType.PROCESSING, staleBefore));
            }
            refreshSuccess.set(1);
            lastSuccessEpochSeconds.set(Instant.now().getEpochSecond());
        } catch (RuntimeException exception) {
            refreshSuccess.set(0);
            log.warn("게시물 동기화 상태 메트릭 집계에 실패했습니다.", exception);
        }
    }
}
