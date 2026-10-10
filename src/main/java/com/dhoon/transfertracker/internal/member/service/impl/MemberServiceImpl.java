package com.dhoon.transfertracker.internal.member.service.impl;

import com.dhoon.transfertracker.internal.config.JwtConfig;
import com.dhoon.transfertracker.internal.member.auth.AccessTokenProvider;
import com.dhoon.transfertracker.internal.member.domain.Member;
import com.dhoon.transfertracker.internal.member.dto.request.MemberFavoriteTeamRegisterRequestDto;
import com.dhoon.transfertracker.internal.member.dto.request.MemberLoginRequestDto;
import com.dhoon.transfertracker.internal.member.dto.request.MemberSignUpRequestDto;
import com.dhoon.transfertracker.internal.member.dto.response.MemberLoginResponseDto;
import com.dhoon.transfertracker.internal.member.dto.response.MemberSignUpResponseDto;
import com.dhoon.transfertracker.internal.member.exception.DuplicateLoginIdException;
import com.dhoon.transfertracker.internal.member.exception.LoginFailException;
import com.dhoon.transfertracker.internal.member.repository.MemberRepository;
import com.dhoon.transfertracker.internal.member.service.MemberService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenProvider accessTokenProvider;

    @Override
    public MemberSignUpResponseDto singUp(MemberSignUpRequestDto request) {
        if (memberRepository.existsByLoginId(request.getLoginId())) {
            throw new DuplicateLoginIdException();
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        Member savedMember = memberRepository.save(Member.signUp(request.getLoginId(), encodedPassword));

        return MemberSignUpResponseDto.of(savedMember);
    }

    @Override
    @Transactional(readOnly = true)
    public MemberLoginResponseDto login(MemberLoginRequestDto request) {
        Member member = memberRepository.findByLoginId(request.getLoginId())
                .orElseThrow(LoginFailException::new);

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new LoginFailException();
        }

        String accessToken = accessTokenProvider.createAccessToken(member.getId());

        return MemberLoginResponseDto.of(member, accessToken);
    }

}
