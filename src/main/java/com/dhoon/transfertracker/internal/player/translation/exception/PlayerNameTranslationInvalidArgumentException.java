package com.dhoon.transfertracker.internal.player.translation.exception;

public class PlayerNameTranslationInvalidArgumentException extends RuntimeException {
    public PlayerNameTranslationInvalidArgumentException() {
        super(
                "플레이어의 한국어 이름 번역과정중, 플레이어의 번역결과 값이 올바르지 않습니다."

        );

    }
}
