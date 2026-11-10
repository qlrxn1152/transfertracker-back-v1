package com.dhoon.transfertracker.internal.member.service.impl;

import com.dhoon.transfertracker.internal.member.auth.AccessTokenProvider;
import com.dhoon.transfertracker.internal.member.domain.Member;
import com.dhoon.transfertracker.internal.member.dto.request.MemberLoginRequestDto;
import com.dhoon.transfertracker.internal.member.dto.request.MemberSignUpRequestDto;
import com.dhoon.transfertracker.internal.member.dto.response.MemberLoginResponseDto;
import com.dhoon.transfertracker.internal.member.dto.response.MemberSignUpResponseDto;
import com.dhoon.transfertracker.internal.member.exception.DuplicateLoginIdException;
import com.dhoon.transfertracker.internal.member.exception.LoginFailException;
import com.dhoon.transfertracker.internal.member.repository.MemberRepository;
import com.dhoon.transfertracker.internal.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenProvider accessTokenProvider;

    /**
     * 신규 회원을 등록한다.
     *
     * <p>로그인 ID의 중복 여부를 검증하고,
     * 비밀번호를 암호화한 후 회원 정보를 저장한다.
     * 신규 회원의 기본 권한은 USER로 설정된다.</p>
     *
     * @param request 사용자가 입력한 회원가입 요청 정보
     * @return 가입된 회원의 정보 응답 DTO
     * @throws DuplicateLoginIdException 이미 등록된 로그인 ID인 경우
     */
    @Override
    public MemberSignUpResponseDto singUp(MemberSignUpRequestDto request) {
        if (memberRepository.existsByLoginId(request.getLoginId())) {
            throw new DuplicateLoginIdException();
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        Member savedMember = memberRepository.save(Member.signUp(request.getLoginId(), encodedPassword));

        return MemberSignUpResponseDto.of(savedMember);
    }



    /**
     * 회원 인증을 수행하고 JWT Access Token을 발급한다.
     *
     * <p>로그인 ID로 회원을 조회한 후 입력한 비밀번호를 검증한다.
     * 인증에 성공하면 회원 정보와 JWT Access Token을 반환한다.</p>
     *
     * @param request 로그인 요청 정보
     * @return 인증된 회원 정보와 JWT Access Token
     * @throws LoginFailException 로그인 ID가 존재하지 않거나 비밀번호가 일치하지 않는 경우
     */
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
