package com.dhoon.transfertracker.internal.admin.exception;

public class AdminSyncFailedException extends RuntimeException {

    public AdminSyncFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
