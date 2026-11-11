package com.dhoon.transfertracker.internal.member.exception;

public class NotFoundMemberFavoriteTeamException extends RuntimeException {
    public NotFoundMemberFavoriteTeamException() {
        super("존재하지 않는 회원의 관심팀 입니다.");
    }
}
