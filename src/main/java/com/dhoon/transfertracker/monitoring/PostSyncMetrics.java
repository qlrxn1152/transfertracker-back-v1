package com.dhoon.transfertracker.monitoring;

import com.dhoon.transfertracker.internal.post.domain.PostSyncStep;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostSyncMetrics {

    private final MeterRegistry meterRegistry;

    public void success(PostSyncStep step) {
        record(step, "success");
    }

    public void failure(PostSyncStep step) {
        record(step, "failure");
    }

    private void record(PostSyncStep step, String result) {
        Counter.builder("veyro.post.sync")
                .description("Post synchronization step outcomes after the database transaction commits")
                .tags("step", step.name().toLowerCase(), "result", result)
                .register(meterRegistry)
                .increment();
    }
}
