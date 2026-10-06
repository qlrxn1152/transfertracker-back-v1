package com.dhoon.transfertracker.external.football.dto;

import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.dto.response.TeamItemResponseDto;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LeagueTeamsResponseDto {
    private LeagueCode leagueCode;
    private List<TeamItemResponseDto> teams;

    public static LeagueTeamsResponseDto of(LeagueCode leagueCode, List<TeamItemResponseDto> teams) {
        return new LeagueTeamsResponseDto(leagueCode, teams);
    }
}
