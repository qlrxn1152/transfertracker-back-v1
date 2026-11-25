package com.dhoon.transfertracker.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationMetrics {

    private final MeterRegistry meterRegistry;

    public void success(String channel) {
        record(channel, "success");
    }

    public void failure(String channel) {
        record(channel, "failure");
    }

    private void record(String channel, String result) {
        Counter.builder("veyro.notification.send")
                .description("Firebase send API outcomes; this does not represent device delivery")
                .tags("channel", channel, "result", result)
                .register(meterRegistry)
                .increment();
    }
}
