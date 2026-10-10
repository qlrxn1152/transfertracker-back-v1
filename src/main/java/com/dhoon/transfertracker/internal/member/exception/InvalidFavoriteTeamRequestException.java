package com.dhoon.transfertracker.internal.member.exception;

public class InvalidFavoriteTeamRequestException extends RuntimeException {
    public InvalidFavoriteTeamRequestException() {
        super("중복된 관심 팀이 포함되어 있습니다.");
    }
}
