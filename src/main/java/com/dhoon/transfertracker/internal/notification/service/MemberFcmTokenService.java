package com.dhoon.transfertracker.internal.notification.service;

import com.dhoon.transfertracker.internal.notification.domain.FcmPlatform;
import com.dhoon.transfertracker.internal.notification.dto.request.MemberFcmTokenRequestDto;

public interface MemberFcmTokenService {

    void createToken(Long memberId, MemberFcmTokenRequestDto requestDto);
}
