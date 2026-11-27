package com.dhoon.transfertracker.internal.fixture.dto.record;

import com.dhoon.transfertracker.internal.fixture.domain.FixtureLineupPlayer;
import com.dhoon.transfertracker.internal.fixture.domain.FixtureLineupPlayerStatus;
import com.dhoon.transfertracker.internal.player.domain.Player;

public record FixtureLineupPlayerResponse(
        Long playerId,
        Long apiFootballId,
        String playerName,
        String displayName,
        String photoUrl,
        Integer number,
        String position,
        String grid,
        FixtureLineupPlayerStatus status
) {

    public static FixtureLineupPlayerResponse from(
            FixtureLineupPlayer lineupPlayer
    ) {
        Player player = lineupPlayer.getPlayer();

        return new FixtureLineupPlayerResponse(
                player.getId(),
                player.getApiFootballId(),
                player.getPlayerName(),
                player.getDisplayName(),
                "https://media.api-sports.io/football/players/"
                        + player.getApiFootballId() + ".png",
                lineupPlayer.getNumber(),
                lineupPlayer.getPosition(),
                lineupPlayer.getGrid(),
                lineupPlayer.getStatus()
        );
    }
}