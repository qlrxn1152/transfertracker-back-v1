package com.dhoon.transfertracker.external.openai.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PlayerNameTranslationBatchResult {

    private List<PlayerNameTranslationResult> players;

    public static PlayerNameTranslationBatchResult of(
            List<PlayerNameTranslationResult> players
    ) {

        return new PlayerNameTranslationBatchResult(
                players
        );
    }

    public static PlayerNameTranslationBatchResult empty() {

        return new PlayerNameTranslationBatchResult(
                new ArrayList<>()
        );
    }
}