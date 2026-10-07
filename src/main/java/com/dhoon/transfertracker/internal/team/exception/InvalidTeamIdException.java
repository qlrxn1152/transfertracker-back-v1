package com.dhoon.transfertracker.internal.team.exception;

public class InvalidTeamIdException extends RuntimeException {
    public InvalidTeamIdException() {
        super("잘못된 TeamId 값입니다.");
    }
}
