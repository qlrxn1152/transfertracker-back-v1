package com.dhoon.transfertracker.internal.member.exception;

public class AlreadyRegisteredFavoriteTeamException extends RuntimeException {
    public AlreadyRegisteredFavoriteTeamException() {
        super("이미 등록한 관심팀이 존재합니다.");
    }
}
