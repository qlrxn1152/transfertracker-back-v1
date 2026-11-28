package com.dhoon.transfertracker.internal.team.query.dto.response.record;

import java.util.List;

public record TeamsResponseDto(List<TeamItemResponseDto> teams) {
    public static TeamsResponseDto of(List<TeamItemResponseDto> teams) {
        return new TeamsResponseDto(teams);
    }

}
