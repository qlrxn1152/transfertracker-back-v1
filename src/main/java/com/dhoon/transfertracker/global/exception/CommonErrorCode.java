package com.dhoon.transfertracker.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

    INVALID_REQUEST(
            HttpStatus.BAD_REQUEST,
            "INVALID_REQUEST",
            "잘못된 요청입니다."
    ),

    INVALID_PARAMETER_TYPE(
            HttpStatus.BAD_REQUEST,
            "INVALID_PARAMETER_TYPE",
            "잘못된 파라미터 타입입니다."
    ),

    MISSING_PARAMETER(
            HttpStatus.BAD_REQUEST,
            "MISSING_PARAMETER",
            "필수 파라미터가 누락되었습니다."
    ),

    INVALID_REQUEST_BODY(
            HttpStatus.BAD_REQUEST,
            "INVALID_REQUEST_BODY",
            "잘못된 요청 본문입니다."
    ),

    RESOURCE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "RESOURCE_NOT_FOUND",
            "요청한 리소스를 찾을 수 없습니다."
    ),

    METHOD_NOT_ALLOWED(
            HttpStatus.METHOD_NOT_ALLOWED,
            "METHOD_NOT_ALLOWED",
            "지원하지 않는 HTTP 메서드입니다."
    ),

    UNSUPPORTED_MEDIA_TYPE(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            "UNSUPPORTED_MEDIA_TYPE",
            "지원하지 않는 미디어 타입입니다."
    ),

    INTERNAL_SERVER_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_SERVER_ERROR",
            "서버 내부 오류가 발생했습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}