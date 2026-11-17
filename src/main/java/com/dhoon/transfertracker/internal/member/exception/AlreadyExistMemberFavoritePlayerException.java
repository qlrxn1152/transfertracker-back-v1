package com.dhoon.transfertracker.internal.member.exception;

public class AlreadyExistMemberFavoritePlayerException extends RuntimeException {
    public AlreadyExistMemberFavoritePlayerException() {
        super("이미 해당 선수를 팔로우중입니다.");
    }
}
