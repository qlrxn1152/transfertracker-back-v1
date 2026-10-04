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
public class TeamTransfersSaveResponseDto {

    private List<TeamTransferSaveResponseDto> teamTransfers = new ArrayList<>();

    public static TeamTransfersSaveResponseDto of(List<TeamTransferSaveResponseDto> teamTransfers) {
        return new TeamTransfersSaveResponseDto(teamTransfers);
    }
}
