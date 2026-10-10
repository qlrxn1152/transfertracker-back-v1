package com.dhoon.transfertracker.internal.member.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MemberFavoriteTeamRegisterRequestDto {

    @NotEmpty
    private List<@NotNull Long> teamIds;
}