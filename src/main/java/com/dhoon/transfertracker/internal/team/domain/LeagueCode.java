package com.dhoon.transfertracker.internal.team.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Objects;

@Getter
@RequiredArgsConstructor
public enum LeagueCode {

    EPL(39L),
    LA_LIGA(140L),
    BUNDESLIGA(78L),
    SERIE_A(135L),
    LIGUE_1(61L),
    K_LEAGUE(292L);

    private final long apiFootballLeagueId;

    public static LeagueCode fromApiFootballLeagueId(Long leagueApiId) {

        for (LeagueCode leagueCode : values()) {

            if ( leagueCode.getApiFootballLeagueId() == leagueApiId ) {
                return leagueCode;
            }

        }

        throw new IllegalArgumentException("Invalid League API ID: " + leagueApiId);
    }

    public int resolveSeason(LocalDate date) {
        int year = date.getYear();

        if (this == K_LEAGUE) {
            return year;
        }

        return date.getMonthValue() < 7 ? year - 1 : year;
    }

    public static boolean isSupported(Long leagueApiId) {
        return Arrays.stream(values())
                .anyMatch(league ->
                        Objects.equals(league.getApiFootballLeagueId(), leagueApiId)
                );
    }
}
