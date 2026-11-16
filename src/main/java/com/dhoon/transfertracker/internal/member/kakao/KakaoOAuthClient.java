package com.dhoon.transfertracker.internal.member.kakao;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Component
@RequiredArgsConstructor
public class KakaoOAuthClient {

    private static final String AUTHORIZE_URL = "https://kauth.kakao.com/oauth/authorize";
    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String PROFILE_URL = "https://kapi.kakao.com/v2/user/me";

    private final RestClient.Builder restClientBuilder;

    @Value("${app.oauth.kakao.client-id:}")
    private String clientId;

    @Value("${app.oauth.kakao.client-secret:}")
    private String clientSecret;

    @Value("${app.oauth.kakao.redirect-uri:}")
    private String redirectUri;

    public String authorizationUrl(String state) {
        requireConfiguration();

        return AUTHORIZE_URL + "?response_type=code"
                + "&client_id=" + encode(clientId)
                + "&redirect_uri=" + encode(redirectUri)
                + "&state=" + encode(state);
    }

    public String getKakaoId(String authorizationCode) {
        requireConfiguration();

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("redirect_uri", redirectUri);
        form.add("code", authorizationCode);
        if (!clientSecret.isBlank()) form.add("client_secret", clientSecret);

        JsonNode tokenResponse = restClientBuilder.build().post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(JsonNode.class);

        String accessToken = tokenResponse == null ? "" : tokenResponse.path("access_token").asText();
        if (accessToken.isBlank()) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "카카오 인증 토큰을 받지 못했습니다.");
        }

        JsonNode profileResponse = restClientBuilder.build().get()
                .uri(PROFILE_URL)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(JsonNode.class);

        String kakaoId = profileResponse == null ? "" : profileResponse.path("id").asText();
        if (kakaoId.isBlank()) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "카카오 사용자 정보를 확인하지 못했습니다.");
        }

        return kakaoId;
    }

    private void requireConfiguration() {
        if (clientId.isBlank() || redirectUri.isBlank()) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "카카오 로그인이 아직 설정되지 않았습니다.");
        }
    }

    private String encode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
