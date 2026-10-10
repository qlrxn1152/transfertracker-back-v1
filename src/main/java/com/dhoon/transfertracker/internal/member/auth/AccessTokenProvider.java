package com.dhoon.transfertracker.internal.member.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Instant;


@Component
@RequiredArgsConstructor
public class AccessTokenProvider {

    private final JwtEncoder jwtEncoder;

    @Value("${jwt.access-token-expiration-seconds}")
    private long accessTokenExpirationSeconds;


    public String createAccessToken(
            Long memberId
    ) {

        Instant now =
                Instant.now();

        JwtClaimsSet claims =
                JwtClaimsSet.builder()
                        .subject(
                                memberId.toString()
                        )
                        .issuedAt(
                                now
                        )
                        .expiresAt(
                                now.plusSeconds(
                                        accessTokenExpirationSeconds
                                )
                        )
                        .build();


        return jwtEncoder
                .encode(
                        JwtEncoderParameters.from(
                                claims
                        )
                )
                .getTokenValue();
    }
}