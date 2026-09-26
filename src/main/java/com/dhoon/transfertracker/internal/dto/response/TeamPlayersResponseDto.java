package com.dhoon.transfertracker.internal.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TeamPlayersResponseDto {

    private List<TeamPlayerItemResponseDto> teamPlayers = new ArrayList<>();

    public static TeamPlayersResponseDto of(List<TeamPlayerItemResponseDto> players) {
        return new TeamPlayersResponseDto(players);
    }

}
