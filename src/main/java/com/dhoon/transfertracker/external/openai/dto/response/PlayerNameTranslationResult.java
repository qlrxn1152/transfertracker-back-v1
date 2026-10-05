package com.dhoon.transfertracker.external.openai.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PlayerNameTranslationResult {

    private Long playerId;
    private String playerNameKo;

    public static PlayerNameTranslationResult of(
            Long playerId,
            String playerNameKo
    ) {

        return new PlayerNameTranslationResult(
                playerId,
                playerNameKo
        );
    }
}