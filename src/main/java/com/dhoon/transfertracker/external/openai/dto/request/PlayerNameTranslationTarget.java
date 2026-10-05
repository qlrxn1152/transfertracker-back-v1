package com.dhoon.transfertracker.external.openai.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PlayerNameTranslationTarget {

    private Long playerId;
    private String playerName;
    private String teamName;
}
