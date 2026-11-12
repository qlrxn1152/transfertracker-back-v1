package com.dhoon.transfertracker.internal.notification.service.impl;

import com.dhoon.transfertracker.internal.member.domain.Member;
import com.dhoon.transfertracker.internal.member.exception.NotFoundMemberException;
import com.dhoon.transfertracker.internal.member.repository.MemberRepository;
import com.dhoon.transfertracker.internal.notification.domain.FcmPlatform;
import com.dhoon.transfertracker.internal.notification.dto.request.MemberFcmTokenRequestDto;
import com.dhoon.transfertracker.internal.notification.repository.MemberFcmTokenRepository;
import com.dhoon.transfertracker.internal.notification.service.MemberFcmTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class MemberFcmTokenServiceImpl implements MemberFcmTokenService {

    private final MemberFcmTokenRepository memberFcmTokenRepository;

    private final MemberRepository memberRepository;

    @Override
    public void createToken(Long memberId, MemberFcmTokenRequestDto requestDto) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(NotFoundMemberException::new);

        FcmPlatform platform = requestDto.platform();
        String token = requestDto.token();

        memberFcmTokenRepository.upsertByToken(member.getId(), platform.name(), token);
    }
}
