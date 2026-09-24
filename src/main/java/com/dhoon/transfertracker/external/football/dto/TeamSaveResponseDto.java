package com.dhoon.transfertracker.external.football.dto;

import com.dhoon.transfertracker.internal.domain.Team;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TeamSaveResponseDto {

    private String teamName;
    private Long teamAPIId;

    public static TeamSaveResponseDto of(Team team) {
        return new TeamSaveResponseDto(team.getTeamName(), team.getApiFootballId());
    }
}
