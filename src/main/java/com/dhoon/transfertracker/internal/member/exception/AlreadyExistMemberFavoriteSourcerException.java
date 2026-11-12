package com.dhoon.transfertracker.internal.member.exception;

public class AlreadyExistMemberFavoriteSourcerException extends RuntimeException {
    public AlreadyExistMemberFavoriteSourcerException() {
        super("이미 해당 기자를 팔로우 하고있는 멤버입니다.");
    }
}
