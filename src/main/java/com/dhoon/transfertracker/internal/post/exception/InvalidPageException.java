package com.dhoon.transfertracker.internal.post.exception;

public class InvalidPageException extends RuntimeException {
    public InvalidPageException() {
        super("페이지는 0 이상이어야 합니다.");
    }
}
