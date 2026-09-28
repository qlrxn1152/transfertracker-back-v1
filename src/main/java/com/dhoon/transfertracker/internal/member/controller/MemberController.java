package com.dhoon.transfertracker.internal.member.controller;

import com.dhoon.transfertracker.internal.member.dto.request.MemberLoginRequestDto;
import com.dhoon.transfertracker.internal.member.dto.request.MemberSignUpRequestDto;
import com.dhoon.transfertracker.internal.member.dto.response.MemberLoginResponseDto;
import com.dhoon.transfertracker.internal.member.dto.response.MemberSignUpResponseDto;
import com.dhoon.transfertracker.internal.member.service.MemberService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    // 우선, valid 없이 ..
    @PostMapping("/api/member/signup")
    public ResponseEntity<MemberSignUpResponseDto> signUp(@RequestBody MemberSignUpRequestDto request) {
        MemberSignUpResponseDto response = memberService.singUp(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/api/member/login")
    public ResponseEntity<MemberLoginResponseDto> login(@RequestBody MemberLoginRequestDto request, HttpSession session) {
        MemberLoginResponseDto response = memberService.login(request);

        session.setAttribute("memberId", response.getMemberId());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }


}
