package com.dhoon.transfertracker.external.openai.exception;

import lombok.Getter;

@Getter
public class OpenAiApiException extends RuntimeException {

    private final OpenAiErrorType errorType;
    private final Integer httpStatusCode;

    public OpenAiApiException(OpenAiErrorType errorType, Integer httpStatusCode, String message, Throwable cause) {
        super(message, cause);

        this.errorType = errorType;
        this.httpStatusCode = httpStatusCode;
    }



}
