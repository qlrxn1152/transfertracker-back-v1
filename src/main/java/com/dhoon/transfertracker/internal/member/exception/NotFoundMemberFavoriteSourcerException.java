package com.dhoon.transfertracker.internal.member.exception;

public class NotFoundMemberFavoriteSourcerException extends RuntimeException {
    public NotFoundMemberFavoriteSourcerException() {
        super("해당 멤버의 해당 기자 팔로우 목록을 찾지 못했습니다.");
    }
}
