package com.dhoon.transfertracker.internal.member.auth;

import com.dhoon.transfertracker.internal.member.domain.Member;
import com.dhoon.transfertracker.internal.member.domain.MemberGrade;
import com.dhoon.transfertracker.internal.member.dto.request.MemberSignUpRequestDto;
import com.dhoon.transfertracker.internal.member.repository.MemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class MemberJwtAuthenticationConverterTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberJwtAuthenticationConverter converter;

    @Test
    @DisplayName(value = "ADMIN 회원은 JWT 인증 시 ROLE_ADMIN 권한을 부여받는다.")
    void convert_adminSuccess() throws Exception {
        // given
        Member member = Member.signUp("adminUser", "password123!");
        ReflectionTestUtils.setField(member, "memberGrade", MemberGrade.ADMIN);
        ReflectionTestUtils.setField(member, "id", 1L);
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject("1")
                .build(); // memberId = 1L => JWT ...

        when(memberRepository.findById(1L))
                .thenReturn(Optional.of(member));

        // when
        AbstractAuthenticationToken token = converter.convert(jwt);

        List<@Nullable String> authorities = token.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        // then
        assertThat(token.getName()).isEqualTo("1");
        assertThat(authorities).containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName(value = "USER 회원은 JWT 인증 시 ROLE_USER 권한을 부여받는다.")
    void convert_userSuccess() throws Exception {
        // given
        Member member = Member.signUp("user", "password123!"); // signUp() 메서드에서, 기본 GRADE = USER 로 설정.
        ReflectionTestUtils.setField(member, "id", 2L);
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject("2")
                .build(); // memberId = 2L => JWT ...

        when(memberRepository.findById(2L))
                .thenReturn(Optional.of(member));

        // when
        AbstractAuthenticationToken token = converter.convert(jwt);

        List<@Nullable String> authorities = token.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        // then
        assertThat(token.getName()).isEqualTo("2");
        assertThat(authorities).containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName(value = "JWT Subject(memberId) 에 숫자가 아니면 인증에 실패한다.")
    void convert_invalidSubject() throws Exception {
        // given
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject("invalid-member-id")
                .build(); // memberId = invalid-member-id => JWT ...

        // when && then
        assertThatThrownBy(() -> converter.convert(jwt))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("유효하지 않은 회원 식별자");

        verifyNoInteractions(memberRepository);
    }

    @Test
    @DisplayName(value = "존재하지 않는 회원일경우, 인증에 실패한다.")
    void convert_notFoundMember() throws Exception {
        // given
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject("999")
                .build(); // memberId = 999L => 해당멤버 존재 X

        when(memberRepository.findById(999L))
                .thenReturn(Optional.empty());

        // when && then
        assertThatThrownBy(() -> converter.convert(jwt))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("유효하지 않은 인증 정보");

        verify(memberRepository, times(1)).findById(999L);
    }


}
