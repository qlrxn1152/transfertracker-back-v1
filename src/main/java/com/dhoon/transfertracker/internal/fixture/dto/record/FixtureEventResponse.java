package com.dhoon.transfertracker.internal.fixture.dto.record;

import com.dhoon.transfertracker.internal.fixture.domain.FixtureEvent;
import com.dhoon.transfertracker.internal.fixture.domain.FixtureEventType;
import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.team.domain.Team;

public record FixtureEventResponse(
        Long fixtureEventId,
        Integer elapsed,
        Integer extra,
        FixtureEventType type,
        String detail,
        String comments,
        TeamResponse team,
        PlayerResponse player,
        PlayerResponse relatedPlayer
) {

    public static FixtureEventResponse from(FixtureEvent event) {
        return new FixtureEventResponse(
                event.getId(),
                event.getElapsed(),
                event.getExtra(),
                event.getType(),
                event.getDetail(),
                event.getComments(),
                TeamResponse.from(event.getTeam()),
                PlayerResponse.from(event.getPlayer()),
                PlayerResponse.from(event.getRelatedPlayer())
        );
    }

    public record TeamResponse(
            Long teamId,
            Long apiFootballId,
            String teamName
    ) {

        public static TeamResponse from(Team team) {
            return new TeamResponse(
                    team.getId(),
                    team.getApiFootballId(),
                    team.getDisplayName()
            );
        }
    }

    public record PlayerResponse(
            Long playerId,
            Long apiFootballId,
            String playerName
    ) {

        public static PlayerResponse from(Player player) {

            if (player == null) {
                return null;
            }

            return new PlayerResponse(
                    player.getId(),
                    player.getApiFootballId(),
                    player.getPlayerName()
            );
        }
    }
}