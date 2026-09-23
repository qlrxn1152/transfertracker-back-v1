package com.dhoon.transfertracker.external.football.dto.team;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TeamInfoResponseDto {

    private String teamName;
    private String teamCode;
    private String teamCountry;
    private String stadiumName;
}
