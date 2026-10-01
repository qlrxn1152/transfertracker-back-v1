package com.dhoon.transfertracker.internal.player.exception;

public class InvalidPlayerSearchPageValueException extends RuntimeException {
    public InvalidPlayerSearchPageValueException() {
        super("페이지 번호는 0 이상이어야 합니다.");
    }
}
