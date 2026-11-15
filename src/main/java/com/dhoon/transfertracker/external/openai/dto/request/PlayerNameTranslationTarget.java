package com.dhoon.transfertracker.external.openai.dto.request;

import com.dhoon.transfertracker.internal.player.domain.Player;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PlayerNameTranslationTarget {

    private Long playerId;
    private String playerName;

    public static PlayerNameTranslationTarget of(Player player) {
        return new PlayerNameTranslationTarget(player.getId(), player.getPlayerName());
    }
}
