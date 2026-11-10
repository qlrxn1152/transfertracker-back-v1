package com.dhoon.transfertracker.internal.transferpost.exception;

public class NotFoundTransferPostException extends RuntimeException {
    public NotFoundTransferPostException() {
        super("존재하지 않는 게시물입니다.");
    }
}
