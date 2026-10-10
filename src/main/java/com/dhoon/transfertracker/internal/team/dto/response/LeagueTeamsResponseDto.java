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
public class LeagueTeamsResponseDto {

    List<TeamItemResponseDto> teams = new ArrayList<>();
    private boolean hasNext;
    private boolean hasPrevious;

    public static LeagueTeamsResponseDto of(List<TeamItemResponseDto> teams, boolean hasNext, boolean hasPrevious) {
        return new LeagueTeamsResponseDto(teams,  hasNext, hasPrevious);
    }
}
