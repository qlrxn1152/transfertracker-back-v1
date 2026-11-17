package com.dhoon.transfertracker.internal.member.controller;

import com.dhoon.transfertracker.internal.member.dto.request.MemberFavoritePlayerRegisterRequestDto;
import com.dhoon.transfertracker.internal.member.dto.response.MemberFavoritePlayerDetailResponseDto;
import com.dhoon.transfertracker.internal.member.dto.response.MemberFavoritePlayerDetailsResponse;
import com.dhoon.transfertracker.internal.member.dto.response.MemberFavoritePlayersResponseDto;
import com.dhoon.transfertracker.internal.member.service.impl.MemberFavoritePlayerService;
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
public class MemberFavoritePlayerController {

    private final MemberFavoritePlayerService memberFavoritePlayerService;

    @PostMapping("/api/members/me/players/follow")
    public ResponseEntity<MemberFavoritePlayerDetailResponseDto> followPlayer(@AuthenticationPrincipal Jwt jwt, MemberFavoritePlayerRegisterRequestDto requestDto) {
        Long memberId = Long.valueOf(jwt.getSubject());

        MemberFavoritePlayerDetailResponseDto response = memberFavoritePlayerService.followPlayer(memberId, requestDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/api/members/me/players/unfollow")
    public ResponseEntity<Void> unfollowPlayer(@AuthenticationPrincipal Jwt jwt, MemberFavoritePlayerRegisterRequestDto requestDto) {
        Long memberId = Long.valueOf(jwt.getSubject());

        memberFavoritePlayerService.unfollowPlayer(memberId, requestDto);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/members/me/players/following")
    public ResponseEntity<MemberFavoritePlayersResponseDto> getMemberFollowPlayers(@AuthenticationPrincipal Jwt jwt) {
        Long memberId = Long.valueOf(jwt.getSubject());

        MemberFavoritePlayersResponseDto response = memberFavoritePlayerService.getMemberFollowPlayers(memberId);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/api/members/me/players/{playerId}/details")
    public ResponseEntity<MemberFavoritePlayerDetailsResponse> getMemberFollowPlayerDetails(@AuthenticationPrincipal Jwt jwt, @PathVariable Long playerId) {
        Long memberId = Long.valueOf(jwt.getSubject());

        MemberFavoritePlayerDetailsResponse response = memberFavoritePlayerService.getMemberFollowPlayerDetails(memberId, playerId);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

}
