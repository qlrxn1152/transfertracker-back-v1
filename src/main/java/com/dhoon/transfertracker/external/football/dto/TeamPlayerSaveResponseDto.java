package com.dhoon.transfertracker.external.football.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TeamPlayerSaveResponseDto {

    private Long playerApiId;
    private String playerName;

    public static TeamPlayerSaveResponseDto of(Long playerApiId, String playerName) {
        return new TeamPlayerSaveResponseDto(playerApiId, playerName);
    }
}
