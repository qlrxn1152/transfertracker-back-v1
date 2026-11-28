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
    K_LEAGUE(292L),

    // 유럽 클럽 대항전
    CHAMPIONS_LEAGUE(2L),
    EUROPA_LEAGUE(3L),
    CONFERENCE_LEAGUE(848L),

    // 국내 컵대회
    FA_CUP(45L),
    EFL_CUP(48L),

    // 국가대표 대회
    WORLD_CUP(1L);

    private final long apiFootballLeagueId;

    public static LeagueCode fromApiFootballLeagueId(Long leagueApiId) {

        for (LeagueCode leagueCode : values()) {

            if (Objects.equals(leagueCode.getApiFootballLeagueId(), leagueApiId)) {
                return leagueCode;
            }
        }

        throw new IllegalArgumentException(
                "Invalid League API ID: " + leagueApiId
        );
    }

    public int resolveSeason(LocalDate date) {

        int year = date.getYear();

        if (this == K_LEAGUE) {
            return year;
        }

        if (this == WORLD_CUP) {
            throw new IllegalStateException(
                    "월드컵은 대회 시즌 정보를 조회해야 합니다."
            );
        }

        return date.getMonthValue() < 7
                ? year - 1
                : year;
    }

    public static boolean isSupported(Long leagueApiId) {

        return Arrays.stream(values())
                .anyMatch(league ->
                        Objects.equals(
                                league.getApiFootballLeagueId(),
                                leagueApiId
                        )
                );
    }

    public boolean isDomesticLeague() {
        return switch (this) {
            case EPL, LA_LIGA, BUNDESLIGA,
                 SERIE_A, LIGUE_1, K_LEAGUE -> true;

            default -> false;
        };
    }
}