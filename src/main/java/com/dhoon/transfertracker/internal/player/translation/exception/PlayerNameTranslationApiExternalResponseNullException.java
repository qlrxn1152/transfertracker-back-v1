package com.dhoon.transfertracker.internal.player.translation.exception;

public class PlayerNameTranslationApiExternalResponseNullException extends RuntimeException {
    public PlayerNameTranslationApiExternalResponseNullException() {
        super(
                "플레이어의 한국어 이름 번역과정중, OPEN AI API 가 전달한 결과가 null 입니다."
        );
    }
}
