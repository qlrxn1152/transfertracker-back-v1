package com.dhoon.transfertracker.external.football.dto;

import com.dhoon.transfertracker.internal.transfer.dto.response.PlayerTransfersResponseDto;
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

    private String teamName;
    private List<PlayerTransfersResponseDto> playersTransfers = new ArrayList<>();

    public static TeamTransfersSaveResponseDto of(String teamName, List<PlayerTransfersResponseDto> teamTransfers) {
        return new TeamTransfersSaveResponseDto(teamName, teamTransfers);
    }
}
