package com.dhoon.transfertracker.internal.transfer.sync.exception;

public class InvalidPlayerTransferDataException extends RuntimeException {
    public InvalidPlayerTransferDataException() {
        super("FootBall API 에서 받은 JsonNode 형식 오류입니다.");
    }
}
