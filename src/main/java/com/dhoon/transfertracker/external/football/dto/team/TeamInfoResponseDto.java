package com.dhoon.transfertracker.external.football.dto.team;

import com.dhoon.transfertracker.internal.team.domain.Team;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TeamInfoResponseDto {

    private Long teamId;
    private String teamName;


    public static TeamInfoResponseDto of(Team team) {
        return new TeamInfoResponseDto(team.getId(), team.getTeamName());
    }

}
