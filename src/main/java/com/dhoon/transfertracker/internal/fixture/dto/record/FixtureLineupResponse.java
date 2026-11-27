package com.dhoon.transfertracker.internal.fixture.dto.record;

import com.dhoon.transfertracker.internal.fixture.domain.FixtureLineup;
import com.dhoon.transfertracker.internal.fixture.domain.FixtureLineupPlayer;
import com.dhoon.transfertracker.internal.fixture.domain.FixtureLineupPlayerStatus;
import com.dhoon.transfertracker.internal.team.domain.Team;

import java.util.List;

public record FixtureLineupResponse(
        Long fixtureLineupId,
        TeamResponse team,
        String formation,
        List<FixtureLineupPlayerResponse> startXI,
        List<FixtureLineupPlayerResponse> substitutes
) {

    public static FixtureLineupResponse from(
            FixtureLineup lineup,
            List<FixtureLineupPlayer> players
    ) {

        List<FixtureLineupPlayerResponse> startXI = players.stream()
                .filter(player -> player.getStatus()
                        == FixtureLineupPlayerStatus.STARTER)
                .map(FixtureLineupPlayerResponse::from)
                .toList();

        List<FixtureLineupPlayerResponse> substitutes = players.stream()
                .filter(player -> player.getStatus()
                        == FixtureLineupPlayerStatus.SUBSTITUTE)
                .map(FixtureLineupPlayerResponse::from)
                .toList();

        return new FixtureLineupResponse(
                lineup.getId(),
                TeamResponse.from(lineup.getTeam()),
                lineup.getFormation(),
                startXI,
                substitutes
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
}