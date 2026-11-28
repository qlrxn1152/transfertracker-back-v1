package com.dhoon.transfertracker.internal.team.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Optional;

@Getter
@RequiredArgsConstructor
public enum TeamCollectionLeague {

    // 기존 지원 리그
    EPL(39L),
    LA_LIGA(140L),
    BUNDESLIGA(78L),
    SERIE_A(135L),
    LIGUE_1(61L),
    K_LEAGUE(292L),

    // 유럽 주요 리그
    PRIMEIRA_LIGA(94L),
    EREDIVISIE(88L),
    BELGIAN_PRO_LEAGUE(144L),
    SUPER_LIG(203L),
    AUSTRIAN_BUNDESLIGA(218L),
    SWISS_SUPER_LEAGUE(207L),
    SCOTTISH_PREMIERSHIP(179L),
    GREEK_SUPER_LEAGUE(197L),
    CZECH_LIGA(345L),
    DANISH_SUPERLIGA(119L),
    ELITESERIEN(103L),
    ALLSVENSKAN(113L),

    // 유럽 기타 리그
    EKSTRAKLASA(106L),
    CROATIAN_HNL(210L),
    SERBIAN_SUPER_LIGA(286L),
    UKRAINIAN_PREMIER_LEAGUE(333L),
    ROMANIAN_LIGA_I(283L),
    HUNGARIAN_NB_I(271L),
    BULGARIAN_FIRST_LEAGUE(172L),
    CYPRUS_FIRST_DIVISION(318L),
    ISRAELI_PREMIER_LEAGUE(383L),
    SLOVAK_SUPER_LIGA(332L),
    SLOVENIAN_PRVA_LIGA(373L),
    AZERBAIJAN_PREMIER_LEAGUE(419L),

    // 하부 리그
    CHAMPIONSHIP(40L),
    LEAGUE_ONE(41L),
    LEAGUE_TWO(42L),
    SEGUNDA_DIVISION(141L),
    BUNDESLIGA_2(79L),
    LIGA_3(80L),
    SERIE_B(136L),
    LIGUE_2(62L),
    K_LEAGUE_2(293L),

    // 유럽 외 리그
    SAUDI_PRO_LEAGUE(307L),
    MLS(253L),
    BRAZIL_SERIE_A(71L),
    ARGENTINA_PRIMERA_DIVISION(128L),
    J_LEAGUE(98L),
    LIGA_MX(262L),
    QATAR_STARS_LEAGUE(305L),
    UAE_PRO_LEAGUE(301L),
    CHINESE_SUPER_LEAGUE(169L),
    A_LEAGUE(188L);

    private final long apiFootballLeagueId;

    public static Optional<TeamCollectionLeague> fromApiFootballLeagueId(Long leagueApiId) {
        return Arrays.stream(values())
                .filter(league -> league.getApiFootballLeagueId() == leagueApiId)
                .findFirst();
    }
}
