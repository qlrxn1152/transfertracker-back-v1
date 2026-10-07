package com.dhoon.transfertracker.internal.team.exception;

public class InvalidLeagueCodeValueException extends RuntimeException {
    public InvalidLeagueCodeValueException() {
        super("잘못된 LeagueCode 값입니다.");
    }
}
