package com.dhoon.transfertracker.internal.member.controller;

import com.dhoon.transfertracker.internal.member.dto.request.MemberLoginRequestDto;
import com.dhoon.transfertracker.internal.member.dto.request.MemberSignUpRequestDto;
import com.dhoon.transfertracker.internal.member.dto.response.MemberLoginResponseDto;
import com.dhoon.transfertracker.internal.member.dto.response.MemberSignUpResponseDto;
import com.dhoon.transfertracker.internal.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/api/members/signup")
    public ResponseEntity<MemberSignUpResponseDto> signUp(@Valid @RequestBody MemberSignUpRequestDto request) {
        MemberSignUpResponseDto response = memberService.singUp(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/api/members/login")
    public ResponseEntity<MemberLoginResponseDto> login(@Valid @RequestBody MemberLoginRequestDto request) {
        MemberLoginResponseDto response = memberService.login(request);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/api/members/me")
    public ResponseEntity<Long> getMyMemberId(@AuthenticationPrincipal Jwt jwt) {
        Long memberId = Long.valueOf(jwt.getSubject());

        return ResponseEntity.ok(memberId);
    }


}
