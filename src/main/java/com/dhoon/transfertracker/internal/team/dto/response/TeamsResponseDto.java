package com.dhoon.transfertracker.internal.team.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamsResponseDto {

    List<TeamItemResponseDto> teams = new ArrayList<>();

    public static TeamsResponseDto of(List<TeamItemResponseDto> teams) {
        return new TeamsResponseDto(teams);
    }
}
