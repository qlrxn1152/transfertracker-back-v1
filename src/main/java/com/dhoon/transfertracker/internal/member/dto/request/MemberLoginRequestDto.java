package com.dhoon.transfertracker.internal.member.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MemberLoginRequestDto {

    private String loginId;
    private String password;
}
