package com.dhoon.transfertracker.internal.transfer.exception;

public class InvalidTransferSearchPageValueException extends RuntimeException {
    public InvalidTransferSearchPageValueException() {
        super("페이지 번호는 0 이상이어야 합니다.");
    }
}
