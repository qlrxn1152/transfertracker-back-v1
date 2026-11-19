package com.dhoon.transfertracker.internal.post.exception;

public class NotFoundPostException extends RuntimeException {
    public NotFoundPostException() {
        super("존재하지 않는 게시물입니다.");
    }
}
