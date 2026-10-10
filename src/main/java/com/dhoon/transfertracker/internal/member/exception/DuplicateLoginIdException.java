package com.dhoon.transfertracker.internal.member.exception;

public class DuplicateLoginIdException extends RuntimeException {
    public DuplicateLoginIdException() {
        super("이미 존재하는 LoginId 입니다.");
    }
}
