package com.dhoon.transfertracker.internal.player.dto.record;
import com.dhoon.transfertracker.internal.player.domain.PlayerRanking;
public record PlayerRankingResponse(Integer ranking, Long playerId, Long apiFootballId, String playerName, String photoUrl, Integer value) {
    public static PlayerRankingResponse from(PlayerRanking r) {
        return new PlayerRankingResponse(r.getRanking(), r.getPlayer().getId(), r.getPlayer().getApiFootballId(), r.getPlayer().getDisplayName(), "https://media.api-sports.io/football/players/" + r.getPlayer().getApiFootballId() + ".png", r.getValue());
    }
}
