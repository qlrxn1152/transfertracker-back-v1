package com.dhoon.transfertracker.internal.member.controller;

import com.dhoon.transfertracker.internal.member.dto.request.MemberFavoriteTeamRegisterRequestDto;
import com.dhoon.transfertracker.internal.member.dto.request.MemberLoginRequestDto;
import com.dhoon.transfertracker.internal.member.dto.request.MemberSignUpRequestDto;
import com.dhoon.transfertracker.internal.member.dto.response.MemberFavoriteTeamItemResponseDto;
import com.dhoon.transfertracker.internal.member.dto.response.MemberFavoriteTeamsResponseDto;
import com.dhoon.transfertracker.internal.member.dto.response.MemberLoginResponseDto;
import com.dhoon.transfertracker.internal.member.dto.response.MemberSignUpResponseDto;
import com.dhoon.transfertracker.internal.member.service.MemberService;
import com.dhoon.transfertracker.internal.member.service.impl.MemberFavoriteTeamService;
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
    private final MemberFavoriteTeamService memberFavoriteTeamService;

    @PostMapping("/api/member/signup")
    public ResponseEntity<MemberSignUpResponseDto> signUp(@Valid @RequestBody MemberSignUpRequestDto request) {
        MemberSignUpResponseDto response = memberService.singUp(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/api/member/login")
    public ResponseEntity<MemberLoginResponseDto> login(@Valid @RequestBody MemberLoginRequestDto request) {
        MemberLoginResponseDto response = memberService.login(request);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/api/member/me")
    public ResponseEntity<Long> getMyMemberId(@AuthenticationPrincipal Jwt jwt) {
        Long memberId = Long.valueOf(jwt.getSubject());

        return ResponseEntity.ok(memberId);
    }

    @PostMapping("/api/member/favorite-teams")
    public ResponseEntity<Void> registerFavoriteTeams(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody MemberFavoriteTeamRegisterRequestDto request) {
        memberFavoriteTeamService.registerFavoriteTeams(Long.valueOf(jwt.getSubject()), request);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/api/member/favorite-teams")
    public ResponseEntity<MemberFavoriteTeamsResponseDto> getFavoriteTeams(@AuthenticationPrincipal Jwt jwt) {
        MemberFavoriteTeamsResponseDto response = memberFavoriteTeamService.getFavoriteTeams(Long.valueOf(jwt.getSubject()));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/api/member/favorite-teams/{teamId}")
    public ResponseEntity<Void> deleteFavoriteTeams(@AuthenticationPrincipal Jwt jwt, @PathVariable Long teamId) {
        memberFavoriteTeamService.deleteFavoriteTeams(Long.valueOf(jwt.getSubject()), teamId);

        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
