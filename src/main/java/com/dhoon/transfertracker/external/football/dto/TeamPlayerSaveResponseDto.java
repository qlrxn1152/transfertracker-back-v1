package com.dhoon.transfertracker.external.football.dto;

import com.dhoon.transfertracker.internal.teamplayer.domain.TeamPlayer;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TeamPlayerSaveResponseDto {

    private Long playerId;
    private String playerName;
    private Long teamId;
    private String teamName;

    public static TeamPlayerSaveResponseDto of(TeamPlayer teamPlayer) {
        return new TeamPlayerSaveResponseDto(
                teamPlayer.getPlayer().getId(),
                teamPlayer.getPlayer().getDisplayName(),
                teamPlayer.getTeam().getId(),
                teamPlayer.getTeam().getDisplayName()
        );
    }
}
