package com.dhoon.transfertracker.internal.post.exception;

public class InvalidPostIdException extends RuntimeException {
    public InvalidPostIdException() {
        super("게시물 ID를 확인해 주세요.");
    }
}
