package com.dhoon.transfertracker.internal.transfer.exception;

public class InvalidTransferIdException extends RuntimeException {
    public InvalidTransferIdException() {
        super("잘못된 Transfer Id 값입니다.");
    }
}
