package com.dhoon.transfertracker.external.football.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TeamPlayersSaveResponseDto {

    private List<TeamPlayerSaveResponseDto> players = new ArrayList<>();

    public static TeamPlayersSaveResponseDto of(List<TeamPlayerSaveResponseDto> players) {
        return new TeamPlayersSaveResponseDto(players);
    }

}
