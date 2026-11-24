package com.dhoon.transfertracker.external.openai.exception;

public enum OpenAiErrorType {
    BAD_REQUEST,
    AUTHENTICATION_FAILED,
    ACCESS_DENIED,
    RATE_LIMIT_EXCEEDED,
    SERVER_ERROR,
    NETWORK_ERROR,
    INVALID_RESPONSE,
    UNKNOWN;

    public static OpenAiErrorType fromStatusCode(int statusCode) {

        if (statusCode >= 500 && statusCode < 600) {
            return SERVER_ERROR;
        }

        return switch (statusCode) {
            case 400 -> BAD_REQUEST;
            case 401 -> AUTHENTICATION_FAILED;
            case 403 -> ACCESS_DENIED;
            case 429 -> RATE_LIMIT_EXCEEDED;
            default -> UNKNOWN;
        };
    }
}
