package com.dhoon.transfertracker.internal.member.service.impl;

import com.dhoon.transfertracker.internal.member.domain.Member;
import com.dhoon.transfertracker.internal.member.dto.request.MemberLoginRequestDto;
import com.dhoon.transfertracker.internal.member.dto.request.MemberSignUpRequestDto;
import com.dhoon.transfertracker.internal.member.dto.response.MemberLoginResponseDto;
import com.dhoon.transfertracker.internal.member.dto.response.MemberSignUpResponseDto;
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

import java.util.UUID;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public MemberSignUpResponseDto singUp(MemberSignUpRequestDto request) {
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        Member savedMember = memberRepository.save(Member.signUp(request.getLoginId(), encodedPassword));

        return MemberSignUpResponseDto.of(savedMember);
    }

    @Override
    public MemberLoginResponseDto login(MemberLoginRequestDto request) {
        Member member = memberRepository.findByLoginId(request.getLoginId())
                .orElseThrow();

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new LoginFailException();
        }
        return MemberLoginResponseDto.of(member);
    }
}
