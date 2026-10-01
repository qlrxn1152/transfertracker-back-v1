package com.dhoon.transfertracker.internal.player.exception;

public class NotFoundPlayerException extends RuntimeException {
    public NotFoundPlayerException() {
        super("해당 Player 가 존재하지 않습니다.");
    }
}
