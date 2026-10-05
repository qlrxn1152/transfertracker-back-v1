package com.dhoon.transfertracker.internal.teamplayer.dto.response;

import com.dhoon.transfertracker.internal.teamplayer.domain.TeamPlayer;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TeamPlayerItemResponseDto {

    private Long playerId;
    private String playerName;
    private String photoUrl;

    public static TeamPlayerItemResponseDto of(TeamPlayer teamPlayer) {
        return new TeamPlayerItemResponseDto(
                teamPlayer.getPlayer().getId(),
                teamPlayer.getPlayer().getDisplayName(),
                "https://media.api-sports.io/football/players/" + teamPlayer.getPlayer().getApiFootballId() + ".png"
        );
    }
}
