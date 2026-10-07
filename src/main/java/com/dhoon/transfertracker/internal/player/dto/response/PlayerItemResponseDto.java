package com.dhoon.transfertracker.internal.player.dto.response;

import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.teamplayer.domain.TeamPlayer;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerItemResponseDto {

    private Long playerId;
    private String playerName;
    private String photoUrl;
    private String teamName;

    public static PlayerItemResponseDto of(Player player, Team team) {
        return new PlayerItemResponseDto(
                player.getId(),
                player.getDisplayName(),
                "https://media.api-sports.io/football/players/" + player.getApiFootballId() + ".png",
                team.getDisplayName()
                );
    }

    public static PlayerItemResponseDto of(TeamPlayer teamPlayer) {
        return new PlayerItemResponseDto(
                teamPlayer.getPlayer().getId(),
                teamPlayer.getPlayer().getDisplayName(),
                "https://media.api-sports.io/football/players/" + teamPlayer.getPlayer().getApiFootballId() + ".png",
                teamPlayer.getTeam().getDisplayName()
        );
    }

    public static PlayerItemResponseDto of(Player player) {
        return new PlayerItemResponseDto(
                player.getId(),
                player.getDisplayName(),
                "https://media.api-sports.io/football/players/" + player.getApiFootballId() + ".png",
                null
        );
    }
}
