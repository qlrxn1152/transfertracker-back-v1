package com.dhoon.transfertracker.internal.member.exception;

public class NotFoundMemberFavoritePlayerException extends RuntimeException {
    public NotFoundMemberFavoritePlayerException() {
        super("멤버의 해당 플레이어 팔로우를 찾지 못했습니다.");
    }
}
