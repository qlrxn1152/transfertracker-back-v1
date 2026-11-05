package com.dhoon.transfertracker.internal.transfer.exception;

public class NotFoundTransferException extends RuntimeException {
    public NotFoundTransferException() {
        super("해당 이적기록을 찾지 못했습니다.");
    }
}
