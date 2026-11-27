package com.dhoon.transfertracker.internal.player.service;
import com.dhoon.transfertracker.external.football.client.*;
import com.dhoon.transfertracker.internal.player.dto.record.ApiFootballPlayerSeasonStatisticsResponse;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import java.util.*;
@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerSeasonStatisticsSyncService {
    private final ApiFootballHttpClient apiFootballHttpClient;
    private final PlayerSeasonStatisticsSyncTxService txService;
    public void syncPlayerSeasonStatistics(Long playerApiId, Integer season) {
        JsonNode root = apiFootballHttpClient.callExternalPlayerSeasonStatisticsApi(playerApiId, season);
        JsonNode response = StatisticsJson.response(root);
        if (root.path("paging").path("total").asInt() > 1) throw new IllegalStateException("Unexpected pagination for player");
        List<ApiFootballPlayerSeasonStatisticsResponse> records = new ArrayList<>();
        for (JsonNode p : response) {
            if (StatisticsJson.id(p.path("player").path("id")) != playerApiId.longValue()) throw new IllegalArgumentException("Wrong player");
            for (JsonNode s : p.path("statistics")) {
                Long leagueId = StatisticsJson.id(s.path("league").path("id"));
                if (!LeagueCode.isSupported(leagueId)) continue;
                ApiFootballPlayerSeasonStatisticsResponse record = ApiFootballPlayerSeasonStatisticsResponse.from(s.path("team"), p.path("player"), s);
                if (!Objects.equals(record.season(), season)) throw new IllegalArgumentException("Wrong season");
                records.add(record);
            }
        }
        if (!records.isEmpty()) txService.sync(records);
    }
    public void syncLeague(LeagueCode league, Integer season) {
        int page = 1;
        int total;
        do {
            JsonNode root = apiFootballHttpClient.callExternalLeaguePlayerSeasonStatisticsApi(league.getApiFootballLeagueId(), season, page);
            JsonNode response = StatisticsJson.response(root);
            total = root.path("paging").path("total").asInt();
            if (total < 0 || total > 100 || (total > 0 && root.path("paging").path("current").asInt() != page)) throw new IllegalStateException("Invalid paging");
            if (response.isEmpty()) {
                if (page < total) throw new IllegalStateException("Empty intermediate page");
                return;
            }
            List<ApiFootballPlayerSeasonStatisticsResponse> records = new ArrayList<>();
            for (JsonNode p : response) {
                for (JsonNode stat : p.path("statistics")) {
                    if (stat.path("league").path("id").asLong() != league.getApiFootballLeagueId()) continue;
                    ApiFootballPlayerSeasonStatisticsResponse r = ApiFootballPlayerSeasonStatisticsResponse.from(stat.path("team"), p.path("player"), stat);
                    if (!Objects.equals(r.season(), season)) throw new IllegalArgumentException("Wrong season");
                    records.add(r);
                }
            }
            if (!records.isEmpty()) txService.sync(records);
            page++;
        } while (page <= total);
    }

    public void syncAllLeagues() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        for (LeagueCode league : LeagueCode.values()) {
            try { syncLeague(league, league.resolveSeason(today)); }
            catch (Exception e) { log.error("Season statistics sync failed: {}", league, e); }
        }
    }
}
