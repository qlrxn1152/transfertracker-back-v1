package com.dhoon.transfertracker.internal.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MemberSignUpRequestDto {

    @NotBlank
    private String loginId;

    @NotBlank
    private String password;

}
