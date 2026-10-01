package com.dhoon.transfertracker.internal.player.exception;

public class InvalidPlayerIdException extends RuntimeException {
    public InvalidPlayerIdException() {
        super("잘못된 PlayerId 값입니다.");
    }
}
