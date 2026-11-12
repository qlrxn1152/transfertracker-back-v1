package com.dhoon.transfertracker.internal.member.controller;

import com.dhoon.transfertracker.internal.member.dto.request.MemberFavoriteSourcerRegisterRequestDto;
import com.dhoon.transfertracker.internal.member.dto.response.MemberFavoriteSourcersAllPostsResponseDto;
import com.dhoon.transfertracker.internal.member.dto.response.MemberFollowJournalistDetailResponseDto;
import com.dhoon.transfertracker.internal.member.dto.response.MemberFollowJournalistsResponseDto;
import com.dhoon.transfertracker.internal.member.service.impl.MemberFavoriteSourcerService;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.transferpost.dto.response.TransferPostsPageResponseDto;
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
public class MemberFavoriteSourcerController {

    private final MemberFavoriteSourcerService memberFavoriteSourcerService;

    @PostMapping("/api/members/me/journalists/follow")
    public ResponseEntity<MemberFollowJournalistDetailResponseDto> followJournalist(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody MemberFavoriteSourcerRegisterRequestDto requestDto) {
        Long memberId = Long.valueOf(jwt.getSubject());

        MemberFollowJournalistDetailResponseDto response = memberFavoriteSourcerService.followJournalist(memberId, requestDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/api/members/me/journalists/follow")
    public ResponseEntity<Void> unfollowJournalist(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody MemberFavoriteSourcerRegisterRequestDto requestDto) {
        Long memberId = Long.valueOf(jwt.getSubject());

        memberFavoriteSourcerService.unFollowJournalist(memberId, requestDto);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/members/me/journalists/following")
    public ResponseEntity<MemberFollowJournalistsResponseDto> getMemberFollowJournalists(@AuthenticationPrincipal Jwt jwt) {
        Long memberId = Long.valueOf(jwt.getSubject());

        MemberFollowJournalistsResponseDto response = memberFavoriteSourcerService.getMemberFollowJournalists(memberId);

        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/api/journalists/{journalist}/posts")
    public ResponseEntity<TransferPostsPageResponseDto> getJournalistPosts(@AuthenticationPrincipal Jwt jwt, @PathVariable TransferPostSource journalist, @RequestParam(defaultValue = "0") int page) {
        Long memberId = Long.valueOf(jwt.getSubject());

        TransferPostsPageResponseDto response = memberFavoriteSourcerService.getJournalistPosts(memberId, journalist, page);

        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/api/members/me/journalists/posts")
    public ResponseEntity<MemberFavoriteSourcersAllPostsResponseDto> getAllJournalistPosts(@AuthenticationPrincipal Jwt jwt, int page) {
        Long memberId = Long.valueOf(jwt.getSubject());

        MemberFavoriteSourcersAllPostsResponseDto response = memberFavoriteSourcerService.getAllJournalistPosts(memberId, page);

        return ResponseEntity.ok().body(response);
    }




}
