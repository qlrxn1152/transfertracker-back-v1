package com.dhoon.transfertracker.internal.fixture.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FixtureEventType {

    GOAL("Goal"),
    CARD("Card"),
    SUBSTITUTION("subst"),
    VAR("Var");

    private final String apiCode;

    public static FixtureEventType fromApiCode(String apiCode) {

        for (FixtureEventType type : values()) {
            if (type.getApiCode().equalsIgnoreCase(apiCode)) {
                return type;
            }
        }

        throw new IllegalArgumentException(
                "Invalid Fixture Event Type: " + apiCode
        );
    }
}