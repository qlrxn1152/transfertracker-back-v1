package com.dhoon.transfertracker.internal.transferpost.exception;

public class InvalidSourcerException extends RuntimeException {
    public InvalidSourcerException() {
        super("Sourcer 값이 맞지 않습니다.");
    }
}
