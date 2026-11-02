package com.dhoon.transfertracker.internal.member.auth;

import com.dhoon.transfertracker.internal.member.domain.Member;
import com.dhoon.transfertracker.internal.member.domain.MemberGrade;
import com.dhoon.transfertracker.internal.member.exception.NotFoundMemberException;
import com.dhoon.transfertracker.internal.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MemberJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final MemberRepository memberRepository;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Long memberId;

        try {
            memberId = Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException | NullPointerException e) {
            throw new BadCredentialsException("유효하지 않은 회원 식별자", e);
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BadCredentialsException("유효하지 않은 인증 정보"));

        String memberRole = "ROLE_" +  member.getMemberGrade().name();
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(memberRole);

        return new JwtAuthenticationToken(jwt, List.of(authority), memberId.toString());
    }
}
