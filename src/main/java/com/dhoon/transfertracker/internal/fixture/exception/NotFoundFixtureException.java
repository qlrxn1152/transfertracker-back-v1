package com.dhoon.transfertracker.internal.fixture.exception;

public class NotFoundFixtureException extends RuntimeException {
    public NotFoundFixtureException() {
        super("존재하지 않는 경기입니다.");
    }
}
