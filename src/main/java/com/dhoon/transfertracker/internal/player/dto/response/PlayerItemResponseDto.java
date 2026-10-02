package com.dhoon.transfertracker.internal.player.dto.response;

import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.team.domain.Team;
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
    private String teamNameKo;

    public static PlayerItemResponseDto of(Player player, Team team) {
        return new PlayerItemResponseDto(
                player.getId(),
                player.getPlayerName(),
                "https://media.api-sports.io/football/players/" + player.getApiFootballId() + ".png",
                team.getTeamName(),
                team.getTeamNameKo()
                );
    }

    public static PlayerItemResponseDto of(Player player) {
        return new PlayerItemResponseDto(
                player.getId(),
                player.getPlayerName(),
                "https://media.api-sports.io/football/players/" + player.getApiFootballId() + ".png",
                null,
                null
        );
    }
}
