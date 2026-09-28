package com.dhoon.transfertracker.internal.member.service;

import com.dhoon.transfertracker.internal.member.dto.request.MemberLoginRequestDto;
import com.dhoon.transfertracker.internal.member.dto.request.MemberSignUpRequestDto;
import com.dhoon.transfertracker.internal.member.dto.response.MemberLoginResponseDto;
import com.dhoon.transfertracker.internal.member.dto.response.MemberSignUpResponseDto;
import jakarta.servlet.http.HttpServletResponse;

public interface MemberService {

    MemberSignUpResponseDto singUp(MemberSignUpRequestDto request);

    MemberLoginResponseDto login(MemberLoginRequestDto request);
}
