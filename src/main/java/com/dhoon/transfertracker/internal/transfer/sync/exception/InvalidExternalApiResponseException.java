package com.dhoon.transfertracker.internal.transfer.sync.exception;

public class InvalidExternalApiResponseException extends RuntimeException {
    public InvalidExternalApiResponseException() {
        super("외부 응답 데이터를 받아오지 못했습니다.");
    }
}
