package com.dhoon.transfertracker.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String requestId = UUID.randomUUID().toString();

        String httpMethod = request.getMethod();
        String requestURI = request.getRequestURI();

        long startTime = System.nanoTime();

        try {
            MDC.put("requestId", requestId);
            response.setHeader("X-Request-ID", requestId);

            filterChain.doFilter(request, response);

        } finally {

            try {
                int httpStatus = response.getStatus();
                long endTime = System.nanoTime();

                long durationMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);

                String memberId = (String) request.getAttribute("memberId");

                if (memberId != null) {
                    MDC.put("memberId", memberId);
                }

                log.info(
                        "HTTP 요청 처리완료. method={}, uri={}, status={}, durationMs={}",
                        httpMethod,
                        requestURI,
                        httpStatus,
                        durationMs
                );
            } finally {
                MDC.remove("requestId");
                MDC.remove("memberId");
            }

        }

    }
}
