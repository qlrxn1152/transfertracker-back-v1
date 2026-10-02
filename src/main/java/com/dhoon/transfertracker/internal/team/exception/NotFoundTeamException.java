package com.dhoon.transfertracker.internal.team.exception;

public class NotFoundTeamException extends RuntimeException {
    public NotFoundTeamException() {
        super("존재하지 않는 팀 입니다.");
    }
}
