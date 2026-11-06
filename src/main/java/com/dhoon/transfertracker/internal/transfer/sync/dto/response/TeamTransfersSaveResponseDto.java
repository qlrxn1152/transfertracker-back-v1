package com.dhoon.transfertracker.internal.transfer.sync.dto.response;

import com.dhoon.transfertracker.internal.transfer.query.dto.response.PlayerTransfersResponseDto;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TeamTransfersSaveResponseDto {

    private String teamName;
    private List<PlayerTransfersResponseDto> playersTransfers = new ArrayList<>();

    private Set<Long> failedPlayerIds = new HashSet<>();

    private int totalPlayerCount;
    private int successPlayerCount;
    private int failedPlayerCount;

    public static TeamTransfersSaveResponseDto of(
            String teamName,
            List<PlayerTransfersResponseDto> playersTransfers,
            Set<Long> failedPlayerIds,
            int totalPlayerCount)
    {
        return new TeamTransfersSaveResponseDto(
                teamName,
                playersTransfers,

                failedPlayerIds,
                totalPlayerCount,

                playersTransfers.size(),
                totalPlayerCount - playersTransfers.size()
        );
    }
}