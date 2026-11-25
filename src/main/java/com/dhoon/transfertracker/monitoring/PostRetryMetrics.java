package com.dhoon.transfertracker.monitoring;

import com.dhoon.transfertracker.internal.post.domain.PostSyncStep;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostRetryMetrics {

    private final MeterRegistry meterRegistry;

    public void accepted(PostSyncStep step) {
        record(step, "accepted");
    }

    public void completed(PostSyncStep step) {
        record(step, "success");
    }

    public void failed(PostSyncStep step) {
        record(step, "failure");
    }

    private void record(PostSyncStep step, String result) {
        Counter.builder("veyro.post.retry")
                .description("Post retry outcomes")
                .tags("step", step.name().toLowerCase(), "result", result)
                .register(meterRegistry)
                .increment();
    }
}
