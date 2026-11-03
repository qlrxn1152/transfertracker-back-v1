package com.dhoon.transfertracker.internal.player.exception;

public class NotFoundTeamPlayerException extends RuntimeException {
    public NotFoundTeamPlayerException() {
        super("팀에 속한 해당 선수를 찾지 못했습니다.");
    }
}
