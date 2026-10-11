package com.dhoon.transfertracker.internal.analytics.controller;

import com.dhoon.transfertracker.internal.analytics.dto.request.PageViewCreateRequestDto;
import com.dhoon.transfertracker.internal.analytics.service.AnalyticsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @PostMapping("/api/analytics/page-view")
    public ResponseEntity<Void> recordPageView(
            @Valid @RequestBody PageViewCreateRequestDto request,
            HttpServletRequest servletRequest
    ) {
        analyticsService.recordPageView(request, servletRequest.getHeader("User-Agent"));

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
