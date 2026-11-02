package com.dhoon.transfertracker.internal.member.service.impl;

import com.dhoon.transfertracker.internal.member.auth.AccessTokenProvider;
import com.dhoon.transfertracker.internal.member.domain.Member;
import com.dhoon.transfertracker.internal.member.domain.MemberGrade;
import com.dhoon.transfertracker.internal.member.dto.request.MemberLoginRequestDto;
import com.dhoon.transfertracker.internal.member.dto.request.MemberSignUpRequestDto;
import com.dhoon.transfertracker.internal.member.dto.response.MemberLoginResponseDto;
import com.dhoon.transfertracker.internal.member.dto.response.MemberSignUpResponseDto;
import com.dhoon.transfertracker.internal.member.exception.DuplicateLoginIdException;
import com.dhoon.transfertracker.internal.member.exception.LoginFailException;
import com.dhoon.transfertracker.internal.member.repository.MemberRepository;
import com.dhoon.transfertracker.internal.member.service.MemberService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberServiceImplTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AccessTokenProvider accessTokenProvider;

    @InjectMocks
    private MemberServiceImpl memberService;

    @Test
    @DisplayName("중복되지 않은 로그인 ID 로 회원가입에 성공한다.")
    void singUp_success() {
        // given
        MemberSignUpRequestDto request = new MemberSignUpRequestDto("testUser", "password123!");
        when(memberRepository.existsByLoginId(request.getLoginId())) // testUser 라는 아이디를 가진 유저가 존재하지 않는다.
                .thenReturn(false);
        when(passwordEncoder.encode("password123!"))
                .thenReturn("encodedPassword");
        when(memberRepository.save(any(Member.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        MemberSignUpResponseDto response = memberService.singUp(request);


        ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);

        verify(memberRepository).save(captor.capture());
        verify(passwordEncoder).encode("password123!");
        Member savedMember = captor.getValue();

        // then
        assertThat(savedMember.getMemberGrade()).isEqualTo(MemberGrade.USER);
        assertThat(savedMember.getLoginId()).isEqualTo("testUser");
        assertThat(savedMember.getPassword()).isEqualTo("encodedPassword");
    }

    @Test
    @DisplayName(value = "이미 존재하는 LoginId 로 회원가입하면 예외가 발생한다.")
    void signUp_duplicateLoginId() throws Exception {
        // given
        MemberSignUpRequestDto request = new MemberSignUpRequestDto("testUser", "password123!");
        when(memberRepository.existsByLoginId("testUser"))
                .thenReturn(true);

        // when && then
        assertThatThrownBy(() -> memberService.singUp(request))
                .isInstanceOf(DuplicateLoginIdException.class);

        verifyNoInteractions(passwordEncoder);
        verify(passwordEncoder, never()).encode("password123!");
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    @DisplayName("올바른 로그인 ID와 비밀번호로 로그인하면 JWT가 발급된다")
    void login_success() throws Exception {
        // given
        MemberLoginRequestDto request = new MemberLoginRequestDto("testUser", "password123!");

        Member member = Member.signUp("testUser", "encodedPassword");
        ReflectionTestUtils.setField(member, "id", 1L);

        when(memberRepository.findByLoginId("testUser"))
                .thenReturn(Optional.of(member));
        when(passwordEncoder.matches("password123!", "encodedPassword"))
                .thenReturn(true);
        when(accessTokenProvider.createAccessToken(1L))
                .thenReturn("token");

        // when
        MemberLoginResponseDto response = memberService.login(request);

        // then
        assertThat(response.getMemberId()).isEqualTo(1L);
        assertThat(response.getAccessToken()).isEqualTo("token");
        assertThat(response.getLoginId()).isEqualTo("testUser");

        verify(accessTokenProvider, times(1)).createAccessToken(1L);
        verify(passwordEncoder, times(1)).matches("password123!", "encodedPassword");
    }

    @Test
    @DisplayName(value = "비밀번호가 일치하지 않으면 로그인에 실패한다.")
    void login_invalidPassword() throws Exception {
        // given
        MemberLoginRequestDto request = new MemberLoginRequestDto("testUser", "invalidPassword");

        Member member = Member.signUp("testUser", "encodedPassword");
        ReflectionTestUtils.setField(member, "id", 1L);

        when(memberRepository.findByLoginId("testUser"))
                .thenReturn(Optional.of(member));
        when(passwordEncoder.matches("invalidPassword", "encodedPassword"))
                .thenReturn(false);

        // when && then
        assertThatThrownBy(() -> memberService.login(request))
                .isInstanceOf(LoginFailException.class);

        verify(memberRepository, times(1)).findByLoginId("testUser");
        verify(passwordEncoder, times(1)).matches("invalidPassword", "encodedPassword");
        verifyNoInteractions(accessTokenProvider);
    }

    @Test
    @DisplayName(value = "존재하지 않는 로그인 ID로 로그인하면 실패한다.")
    void login_memberNotFound() throws Exception {
        // given
        MemberLoginRequestDto request = new MemberLoginRequestDto("invalidLoginId", "password123!");

        Member member = Member.signUp("testUser", "password123!");
        ReflectionTestUtils.setField(member, "id", 1L);

        when(memberRepository.findByLoginId("invalidLoginId"))
                .thenReturn(Optional.empty());

        // when && then
        assertThatThrownBy(() -> memberService.login(request))
                .isInstanceOf(LoginFailException.class);

        verify(memberRepository, times(1)).findByLoginId("invalidLoginId");
        verifyNoInteractions(accessTokenProvider);
        verifyNoInteractions(passwordEncoder);
    }



}
