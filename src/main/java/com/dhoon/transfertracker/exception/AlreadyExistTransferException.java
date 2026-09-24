package com.dhoon.transfertracker.exception;

public class AlreadyExistTransferException extends RuntimeException {
    public AlreadyExistTransferException() {
        super("이미 존재하는 이적정보입니다.");
    }
}
