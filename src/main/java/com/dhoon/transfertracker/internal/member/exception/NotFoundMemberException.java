package com.dhoon.transfertracker.internal.member.exception;

public class NotFoundMemberException extends RuntimeException {
    public NotFoundMemberException() {
        super("해당 멤버를 찾지 못했습니다.");
    }
}
