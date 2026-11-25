package com.dhoon.transfertracker.monitoring;

import com.dhoon.transfertracker.external.openai.exception.OpenAiApiException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * 외부 API를 실제로 호출한 구간만 측정한다. provider/operation/errorType은 제한된 값만
 * 사용해 요청별 URL·식별자처럼 cardinality가 높은 데이터가 Prometheus label에 들어가지 않게 한다.
 */
@Component
@RequiredArgsConstructor
public class ExternalApiMetrics {

    private final MeterRegistry meterRegistry;

    public <T> T record(String provider, String operation, Supplier<T> call) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            T result = call.get();
            stop(sample, provider, operation, "success", "none");
            return result;
        } catch (RuntimeException exception) {
            stop(sample, provider, operation, "failure", errorType(exception));
            throw exception;
        }
    }

    private void stop(Timer.Sample sample, String provider, String operation, String result, String errorType) {
        sample.stop(Timer.builder("veyro.external.api.duration")
                .description("External API request duration")
                .tags("provider", provider, "operation", operation, "result", result, "error_type", errorType)
                .register(meterRegistry));
    }

    private String errorType(RuntimeException exception) {
        if (exception instanceof OpenAiApiException openAiException) {
            return openAiException.getErrorType().name().toLowerCase(Locale.ROOT);
        }
        if (exception instanceof RestClientResponseException responseException) {
            int status = responseException.getStatusCode().value();
            if (status == 429) return "rate_limit";
            return status >= 500 ? "server_error" : "http_" + (status / 100) + "xx";
        }
        if (exception instanceof ResourceAccessException) return "network";
        return "unknown";
    }
}
