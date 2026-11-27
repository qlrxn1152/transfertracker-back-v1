package com.dhoon.transfertracker.internal.fixture.dto.record;

import com.dhoon.transfertracker.internal.fixture.domain.Fixture;
import com.dhoon.transfertracker.internal.fixture.domain.FixtureEvent;

import java.util.List;

public record FixtureDetailResponse(
        FixtureResponse fixture,
        List<FixtureEventResponse> events,
        List<FixtureLineupResponse> lineups
) {

    public static FixtureDetailResponse from(
            Fixture fixture,
            List<FixtureEvent> events,
            List<FixtureLineupResponse> lineups
    ) {
        return new FixtureDetailResponse(
                FixtureResponse.from(fixture),
                events.stream()
                        .map(FixtureEventResponse::from)
                        .toList(),
                lineups
        );
    }
}