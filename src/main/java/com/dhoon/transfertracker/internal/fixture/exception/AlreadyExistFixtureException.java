package com.dhoon.transfertracker.internal.fixture.exception;

public class AlreadyExistFixtureException extends RuntimeException {
    public AlreadyExistFixtureException() {
        super("이미 존재하는 Fixture 입니다.");
    }
}
