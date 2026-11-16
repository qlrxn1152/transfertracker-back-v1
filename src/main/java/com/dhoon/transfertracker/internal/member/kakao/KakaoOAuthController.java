package com.dhoon.transfertracker.internal.member.kakao;

import com.dhoon.transfertracker.internal.member.dto.response.MemberLoginResponseDto;
import com.dhoon.transfertracker.internal.member.service.MemberService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/kakao")
public class KakaoOAuthController {

    private static final String STATE_COOKIE = "transfertracker_kakao_state";

    private final KakaoOAuthClient kakaoOAuthClient;
    private final MemberService memberService;

    @Value("${app.oauth.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @GetMapping("/authorize")
    public ResponseEntity<Void> authorize(HttpServletResponse response) {
        String state = UUID.randomUUID().toString();
        ResponseCookie cookie = ResponseCookie.from(STATE_COOKIE, state)
                .httpOnly(true)
                .secure(frontendUrl.startsWith("https://"))
                .sameSite("Lax")
                .path("/api/auth/kakao")
                .maxAge(300)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.status(302)
                .location(URI.create(kakaoOAuthClient.authorizationUrl(state)))
                .build();
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @CookieValue(value = STATE_COOKIE, required = false) String expectedState,
            HttpServletResponse response
    ) {
        clearStateCookie(response);

        if (error != null || code == null || !sameState(expectedState, state)) {
            return redirectToLoginError("카카오 로그인이 취소되었거나 만료되었습니다.");
        }

        try {
            String kakaoId = kakaoOAuthClient.getKakaoId(code);
            MemberLoginResponseDto login = memberService.loginWithKakao(kakaoId);
            String fragment = "accessToken=" + encode(login.getAccessToken())
                    + "&memberId=" + login.getMemberId()
                    + "&loginId=" + encode(login.getLoginId())
                    + "&memberGrade=" + encode(login.getMemberGrade().name());

            return ResponseEntity.status(302)
                    .location(URI.create(frontendUrl + "/auth/kakao/callback#" + fragment))
                    .build();
        } catch (Exception exception) {
            return redirectToLoginError("카카오 로그인을 완료하지 못했습니다.");
        }
    }

    private ResponseEntity<Void> redirectToLoginError(String message) {
        return ResponseEntity.status(302)
                .location(URI.create(frontendUrl + "/login?kakaoError=" + encode(message)))
                .build();
    }

    private boolean sameState(String expected, String actual) {
        if (expected == null || actual == null) return false;
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8)
        );
    }

    private void clearStateCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(STATE_COOKIE, "")
                .httpOnly(true)
                .secure(frontendUrl.startsWith("https://"))
                .sameSite("Lax")
                .path("/api/auth/kakao")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
