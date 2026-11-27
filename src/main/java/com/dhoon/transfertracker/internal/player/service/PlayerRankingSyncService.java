package com.dhoon.transfertracker.internal.player.service;
import com.dhoon.transfertracker.external.football.client.*;
import com.dhoon.transfertracker.internal.player.domain.PlayerRankingType;
import com.dhoon.transfertracker.internal.player.dto.record.ApiFootballPlayerRankingResponse;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import java.time.*;
import java.util.*;
@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerRankingSyncService {
    private final ApiFootballHttpClient apiFootballHttpClient;
    private final PlayerRankingSyncTxService txService;
    public void sync(LeagueCode league, Integer season, PlayerRankingType type) {
        JsonNode response = StatisticsJson.response(apiFootballHttpClient.callExternalPlayerRankingApi(league.getApiFootballLeagueId(), season, type));
        if (response.isEmpty()) return;
        List<ApiFootballPlayerRankingResponse> records = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (JsonNode p : response) {
            JsonNode player = p.path("player");
            Long playerId = StatisticsJson.id(player.path("id"));
            if (!seen.add(playerId)) throw new IllegalArgumentException("Duplicate ranking player");
            JsonNode stats = p.path("statistics");
            if (!stats.isArray() || stats.size() != 1) throw new IllegalArgumentException("Ambiguous ranking statistics");
            JsonNode s = stats.get(0);
            if (s.path("league").path("id").asLong() != league.getApiFootballLeagueId() || s.path("league").path("season").asInt() != season) throw new IllegalArgumentException("Wrong league or season");
            Integer value = StatisticsJson.integer(s.path("goals").path(type == PlayerRankingType.GOALS ? "total" : "assists"));
            if (value == null) throw new IllegalArgumentException("Missing ranking value");
            records.add(new ApiFootballPlayerRankingResponse(playerId, StatisticsJson.name(player.path("name")), records.size() + 1, value));
        }
        txService.sync(league, season, type, records);
    }
    public void syncAll() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        for (LeagueCode league : LeagueCode.values()) for (PlayerRankingType type : PlayerRankingType.values()) {
            try { sync(league, league.resolveSeason(today), type); } catch (Exception e) { log.error("Ranking sync failed: {} {}", league, type, e); }
        }
    }
}
