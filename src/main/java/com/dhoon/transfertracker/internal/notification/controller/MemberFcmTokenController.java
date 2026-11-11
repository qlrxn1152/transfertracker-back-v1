package com.dhoon.transfertracker.internal.notification.controller;

import com.dhoon.transfertracker.internal.notification.dto.request.MemberFcmTokenRequestDto;
import com.dhoon.transfertracker.internal.notification.service.MemberFcmTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class MemberFcmTokenController {

    private final MemberFcmTokenService memberFcmTokenService;

    @PostMapping("/api/members/me/fcm-tokens")
    public ResponseEntity<Void> createToken(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody MemberFcmTokenRequestDto requestDto) {
        Long memberId = Long.valueOf(jwt.getSubject());

        memberFcmTokenService.createToken(memberId, requestDto);

        return ResponseEntity.ok().build();
    }
}
