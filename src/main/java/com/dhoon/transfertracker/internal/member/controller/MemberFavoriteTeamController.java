package com.dhoon.transfertracker.internal.member.controller;

import com.dhoon.transfertracker.internal.member.dto.request.MemberFavoriteTeamRegisterRequestDto;
import com.dhoon.transfertracker.internal.member.dto.response.MemberFavoriteTeamCreateResponseDto;
import com.dhoon.transfertracker.internal.member.dto.response.MemberFavoriteTeamDetailResponseDto;
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
public class MemberFavoriteTeamController {

    private final MemberFavoriteTeamService memberFavoriteTeamService;

    @PostMapping("/api/members/favorite-teams")
    public ResponseEntity<MemberFavoriteTeamCreateResponseDto> createFavoriteTeam(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody MemberFavoriteTeamRegisterRequestDto requestDto) {
        Long memberId = Long.valueOf(jwt.getSubject());

        MemberFavoriteTeamCreateResponseDto response = memberFavoriteTeamService.createFavoriteTeam(memberId, requestDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/members/favorite-teams")
    public ResponseEntity<MemberFavoriteTeamDetailResponseDto> getMyFavoriteTeams(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "0") int page) {
        Long memberId = Long.valueOf(jwt.getSubject());

        MemberFavoriteTeamDetailResponseDto response = memberFavoriteTeamService.getMyFavoriteTeams(memberId, page);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/api/members/favorite-teams/{teamId}")
    public ResponseEntity<Void> deleteFavoriteTeam(@AuthenticationPrincipal Jwt jwt, @PathVariable Long teamId) {
        Long memberId = Long.valueOf(jwt.getSubject());

        memberFavoriteTeamService.deleteFavoriteTeam(memberId, teamId);

        return ResponseEntity.noContent().build();
    }

}
