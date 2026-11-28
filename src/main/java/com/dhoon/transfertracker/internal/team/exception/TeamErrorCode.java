package com.dhoon.transfertracker.internal.team.exception;

import com.dhoon.transfertracker.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TeamErrorCode implements ErrorCode {

    TEAM_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "TEAM_NOT_FOUND",
            "존재하지 않는 팀입니다."
    ),

    INVALID_TEAM_ID(
            HttpStatus.BAD_REQUEST,
            "INVALID_TEAM_ID",
            "유효하지 않은 팀 ID입니다."
    ),

    INVALID_LEAGUE_CODE(
            HttpStatus.BAD_REQUEST,
            "INVALID_LEAGUE_CODE",
            "유효하지 않은 리그 코드입니다."
    );




    private final HttpStatus status;
    private final String code;
    private final String message;

}
