package com.dhoon.transfertracker.internal.player.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PlayerNameTranslationResponseDto {

    private int targetCount;
    private int translatedCount;
    private int batchCount;


    public static PlayerNameTranslationResponseDto of(
            int targetCount,
            int translatedCount,
            int batchCount
    ) {

        return new PlayerNameTranslationResponseDto(
                targetCount,
                translatedCount,
                batchCount
        );
    }
}