package com.dhoon.transfertracker.internal.fixture.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FixtureStatus {

    // 경기 시작 전
    TIME_TO_BE_DEFINED("TBD"),       // 경기 시간 미정
    NOT_STARTED("NS"),               // 경기 시작 전

    // 경기 진행 중
    FIRST_HALF("1H"),                // 전반전
    HALF_TIME("HT"),                 // 하프타임
    SECOND_HALF("2H"),               // 후반전
    EXTRA_TIME("ET"),                // 연장전
    BREAK_TIME("BT"),                // 연장전 휴식
    PENALTY_IN_PROGRESS("P"),        // 승부차기 진행 중
    LIVE("LIVE"),                    // 경기 진행 중 (세부 상태 미제공)

    // 경기 종료
    FINISHED("FT"),                  // 정규시간 종료
    AFTER_EXTRA_TIME("AET"),         // 연장전 종료
    AFTER_PENALTY("PEN"),            // 승부차기 종료

    // 경기 예외 상황
    SUSPENDED("SUSP"),               // 경기 중단
    INTERRUPTED("INT"),              // 경기 일시 중단
    POSTPONED("PST"),                // 경기 연기
    CANCELLED("CANC"),               // 경기 취소
    ABANDONED("ABD"),                // 경기 중도 포기
    TECHNICAL_LOSS("AWD"),           // 규정에 따른 패배
    WALKOVER("WO");                  // 부전승
    private final String apiCode;

    public static FixtureStatus fromApiCode(String apiCode) {

        for (FixtureStatus status : values()) {

            if (status.getApiCode().equals(apiCode)) {
                return status;
            }

        }

        throw new IllegalArgumentException("Invalid API code " + apiCode);
    }

}
