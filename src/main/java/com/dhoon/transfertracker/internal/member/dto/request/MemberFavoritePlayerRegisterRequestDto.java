package com.dhoon.transfertracker.internal.member.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MemberFavoritePlayerRegisterRequestDto {

    @NotNull
    private Long playerId;
}
