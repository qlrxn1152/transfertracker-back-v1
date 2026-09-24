package com.dhoon.transfertracker.internal.dto.team;

import com.dhoon.transfertracker.internal.domain.Team;
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
